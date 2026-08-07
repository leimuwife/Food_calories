package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nutrition.client.FastApiClient;
import com.nutrition.client.FastApiProperties;
import com.nutrition.entity.Attachment;
import com.nutrition.entity.RagKnowledgeDocument;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.RagDocumentStatusEnum;
import com.nutrition.mapper.RagKnowledgeDocumentMapper;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.RagKnowledgeService;
import com.nutrition.vo.KnowledgeDocumentVO;
import com.nutrition.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG知识库文档管理服务实现
 *
 * <p>核心流程：
 * 1. 接收前端文件 → 计算MD5 → MySQL查重
 * 2. 上传文件到OSS → 保存附件记录(sys_file) → 获取附件ID
 * 3. 保存文档记录（状态=向量入库中，file_ids=附件ID）
 * 4. 调用Python FastAPI接口，传递文件+doc_id+file_md5
 * 5. Python异步处理完成后回调Java更新文档状态
 *
 * <p>附件存储参考食物图片(diet_item.file_ids)的存储方式：
 * 文件上传到OSS后，将sys_file表主键ID存入rag_knowledge_document.file_ids字段
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagKnowledgeServiceImpl implements RagKnowledgeService {

    private final RagKnowledgeDocumentMapper documentMapper;
    private final FastApiClient fastApiClient;
    private final FastApiProperties fastApiProperties;
    private final AttachmentService attachmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO uploadDocument(MultipartFile file, Long userId) {
        String originalFilename = file.getOriginalFilename();
        log.info("知识库文档上传开始: fileName={}, userId={}", originalFilename, userId);

        try {
            // 1. 计算文件MD5
            String fileMd5 = calculateMd5(file.getBytes());
            log.info("文件MD5计算完成: fileMd5={}", fileMd5);

            // 2. MySQL查重（根据file_md5唯一键查询）
            RagKnowledgeDocument existing = documentMapper.selectOne(
                    new LambdaQueryWrapper<RagKnowledgeDocument>()
                            .eq(RagKnowledgeDocument::getFileMd5, fileMd5)
                            .eq(RagKnowledgeDocument::getDeleteFlag, 0)
            );

            if (existing != null) {
                log.warn("文件重复: fileMd5={}, existingId={}", fileMd5, existing.getId());
                throw new IllegalStateException(BizMsgEnum.RAG_FILE_DUPLICATE.getMessage());
            }

            // 3. 上传文件到OSS，保存附件记录到sys_file表（参考食物图片存储方式）
            Attachment attachment = attachmentService.upload(file, userId, "rag/");
            String fileIds = String.valueOf(attachment.getId());
            log.info("文件已上传OSS: fileName={}, attachmentId={}, url={}",
                    originalFilename, attachment.getId(), attachment.getFileUrl());

            // 4. 保存文档记录（状态=向量入库中，file_ids关联附件表主键）
            RagKnowledgeDocument document = new RagKnowledgeDocument();
            document.setDocName(originalFilename);
            document.setFileMd5(fileMd5);
            document.setUploadUserId(userId);
            document.setStatus(RagDocumentStatusEnum.PROCESSING.getCode());
            document.setRemark("等待Python向量入库处理");
            document.setVectorStoreId("");
            document.setFileIds(fileIds);
            document.setDeleteFlag(0);
            documentMapper.insert(document);
            log.info("文档记录已保存: docId={}, status={}, fileIds={}",
                    document.getId(), RagDocumentStatusEnum.PROCESSING.getDesc(), fileIds);

            // 5. 调用Python FastAPI接口进行向量入库
            try {
                boolean success = fastApiClient.uploadKnowledgeFile(
                        file, document.getId(), fileMd5
                );

                if (!success) {
                    updateStatusDirect(document.getId(), RagDocumentStatusEnum.FAILED,
                            BizMsgEnum.RAG_PYTHON_CALL_FAILED.getMessage() + "，入库异常");
                    throw new RuntimeException(BizMsgEnum.RAG_PYTHON_CALL_FAILED.getMessage());
                }

                log.info("已通知Python进行向量入库: docId={}", document.getId());
            } catch (Exception e) {
                log.error("Python调用异常: docId={}, error={}", document.getId(), e.getMessage(), e);
                updateStatusDirect(document.getId(), RagDocumentStatusEnum.FAILED,
                        BizMsgEnum.RAG_PYTHON_CALL_FAILED.getMessage() + ": " + e.getMessage());
                throw new RuntimeException(BizMsgEnum.RAG_PYTHON_CALL_FAILED.getMessage() + ": " + e.getMessage());
            }

            return convertToVO(document, attachment);

        } catch (IllegalStateException e) {
            log.warn("文档上传校验失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("文档上传异常: file={}, error={}", originalFilename, e.getMessage(), e);
            throw new RuntimeException(BizMsgEnum.RAG_UPLOAD_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    @Override
    public PageVO<KnowledgeDocumentVO> listDocuments(String keyword, int pageNum, int pageSize) {
        log.info("查询知识库文档列表: keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);

        LambdaQueryWrapper<RagKnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagKnowledgeDocument::getDeleteFlag, 0);

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(RagKnowledgeDocument::getDocName, keyword.trim());
        }
        wrapper.orderByDesc(RagKnowledgeDocument::getCreateTime);

        Page<RagKnowledgeDocument> page = documentMapper.selectPage(
                Page.of(pageNum, pageSize), wrapper
        );

        // 批量查询当前页所有文档关联的附件信息（参考DietRecordServiceImpl批量加载模式）
        List<Long> allAttachmentIds = new ArrayList<>();
        for (RagKnowledgeDocument doc : page.getRecords()) {
            allAttachmentIds.addAll(parseFileIds(doc.getFileIds()));
        }

        Map<Long, Attachment> attachmentMap = new HashMap<>();
        if (!allAttachmentIds.isEmpty()) {
            List<Attachment> attachments = attachmentService.batchGetByIds(allAttachmentIds);
            attachmentMap = attachments.stream()
                    .collect(Collectors.toMap(Attachment::getId, a -> a, (a1, a2) -> a1));
        }

        Map<Long, Attachment> finalAttachmentMap = attachmentMap;
        List<KnowledgeDocumentVO> records = page.getRecords().stream()
                .map(doc -> convertToVO(doc, finalAttachmentMap))
                .collect(Collectors.toList());

        return new PageVO<>(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        log.info("删除知识库文档: docId={}", id);

        RagKnowledgeDocument document = documentMapper.selectById(id);
        if (document == null || Integer.valueOf(1).equals(document.getDeleteFlag())) {
            throw new IllegalArgumentException(BizMsgEnum.RAG_DOC_NOT_EXIST.getMessage());
        }

        // 1. 调用Python删除向量
        try {
            fastApiClient.deleteKnowledgeDocument(String.valueOf(id));
            log.info("已通知Python删除向量: docId={}", id);
        } catch (Exception e) {
            log.error("Python删除向量异常: docId={}, error={}", id, e.getMessage(), e);
        }

        // 2. 删除关联的OSS附件（参考食物图片删除方式）
        List<Long> attachmentIds = parseFileIds(document.getFileIds());
        if (!attachmentIds.isEmpty()) {
            for (Long attachmentId : attachmentIds) {
                try {
                    attachmentService.delete(attachmentId);
                    log.info("附件已删除: attachmentId={}", attachmentId);
                } catch (Exception e) {
                    log.error("附件删除异常: attachmentId={}, error={}", attachmentId, e.getMessage(), e);
                }
            }
        }

        // 3. 逻辑删除文档记录
        document.setDeleteFlag(1);
        document.setStatus(RagDocumentStatusEnum.DELETED.getCode());
        documentMapper.updateById(document);
        log.info("文档已删除: docId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocumentStatus(Long docId, Integer status, String vectorStoreId, String message) {
        log.info("更新文档状态回调: docId={}, status={}, vectorStoreId={}", docId, status, vectorStoreId);

        RagKnowledgeDocument document = documentMapper.selectById(docId);
        if (document == null) {
            log.warn("回调文档不存在: docId={}", docId);
            return;
        }

        document.setStatus(status);
        if (vectorStoreId != null && !vectorStoreId.isEmpty()) {
            document.setVectorStoreId(vectorStoreId);
        }
        if (message != null) {
            document.setRemark(message);
        }
        documentMapper.updateById(document);
        log.info("文档状态已更新: docId={}, status={}", docId, RagDocumentStatusEnum.getDescByCode(status));
    }

    /**
     * 直接更新状态（内部方法，用于上传过程中标记失败）
     */
    private void updateStatusDirect(Long docId, RagDocumentStatusEnum status, String remark) {
        try {
            RagKnowledgeDocument document = documentMapper.selectById(docId);
            if (document != null) {
                document.setStatus(status.getCode());
                document.setRemark(remark);
                documentMapper.updateById(document);
            }
        } catch (Exception e) {
            log.error("{}: docId={}", BizMsgEnum.RAG_STATUS_UPDATE_FAILED.getMessage(), docId, e);
        }
    }

    /**
     * 将实体转换为VO（上传时使用，直接传入附件对象）
     */
    private KnowledgeDocumentVO convertToVO(RagKnowledgeDocument doc, Attachment attachment) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        vo.setId(doc.getId());
        vo.setFileName(doc.getDocName());
        vo.setFileMd5(doc.getFileMd5());
        vo.setStatus(doc.getStatus());
        if (attachment != null) {
            vo.setFileSize(attachment.getFileSize());
            vo.setFileUrl(attachment.getFileUrl());
        } else {
            vo.setFileSize(0L);
        }
        if (doc.getCreateTime() != null) {
            vo.setUploadTime(doc.getCreateTime());
        } else {
            vo.setUploadTime(LocalDateTime.now());
        }
        return vo;
    }

    /**
     * 将实体转换为VO（列表查询时使用，从批量加载的附件Map中获取附件信息）
     */
    private KnowledgeDocumentVO convertToVO(RagKnowledgeDocument doc, Map<Long, Attachment> attachmentMap) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        vo.setId(doc.getId());
        vo.setFileName(doc.getDocName());
        vo.setFileMd5(doc.getFileMd5());
        vo.setStatus(doc.getStatus());

        // 从file_ids解析附件ID，查询附件URL和文件大小
        List<Long> fileIds = parseFileIds(doc.getFileIds());
        if (!fileIds.isEmpty()) {
            Attachment attachment = attachmentMap.get(fileIds.get(0));
            if (attachment != null) {
                vo.setFileSize(attachment.getFileSize());
                vo.setFileUrl(attachment.getFileUrl());
            } else {
                vo.setFileSize(0L);
            }
        } else {
            vo.setFileSize(0L);
        }

        if (doc.getCreateTime() != null) {
            vo.setUploadTime(doc.getCreateTime());
        } else {
            vo.setUploadTime(LocalDateTime.now());
        }
        return vo;
    }

    /**
     * 解析file_ids字符串为Long列表（参考DietRecordServiceImpl.parseFileIds）
     *
     * @param fileIdsStr 逗号分隔的附件ID字符串，如 "123456" 或 "123,456"
     * @return 附件ID列表
     */
    private List<Long> parseFileIds(String fileIdsStr) {
        if (!StringUtils.hasText(fileIdsStr)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String id : fileIdsStr.split(",")) {
            try {
                result.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("附件ID格式错误: fileId={}", id);
            }
        }
        return result;
    }

    /**
     * 计算文件内容的MD5值
     */
    private String calculateMd5(byte[] content) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(content);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
