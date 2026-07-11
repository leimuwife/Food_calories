package com.nutrition.service.impl;

import com.nutrition.entity.Attachment;
import com.nutrition.mapper.AttachmentMapper;
import com.nutrition.service.AttachmentService;
import com.nutrition.util.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 附件服务实现类
 * 实现文件上传、下载、删除等附件管理功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final OssUtil ossUtil;

    /**
     * 上传单个文件（不带前缀）
     *
     * @param file   上传的文件
     * @param userId 用户ID
     * @return Attachment 附件实体
     * @throws IOException 上传异常
     */
    @Override
    @Transactional
    public Attachment upload(MultipartFile file, Long userId) throws IOException {
        return upload(file, userId, null);
    }

    /**
     * 上传单个文件（带前缀）
     * 将文件上传到OSS，并保存附件记录到数据库
     *
     * @param file   上传的文件
     * @param userId 用户ID
     * @param prefix 文件路径前缀，如 "diet/"
     * @return Attachment 附件实体
     * @throws IOException 上传异常
     */
    @Override
    @Transactional
    public Attachment upload(MultipartFile file, Long userId, String prefix) throws IOException {
        String url = ossUtil.upload(file, prefix);

        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileSuffix(getFileSuffix(file.getOriginalFilename()));
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl(url);
        attachment.setStorageType(2);
        attachment.setUploadUserId(userId);
        attachment.setDeleteFlag(0);

        attachmentMapper.insert(attachment);
        log.info("附件保存成功: id={}, url={}", attachment.getId(), url);

        return attachment;
    }

    /**
     * 批量上传文件（不带前缀）
     *
     * @param files  文件列表
     * @param userId 用户ID
     * @return List<Attachment> 附件实体列表
     * @throws IOException 上传异常
     */
    @Override
    @Transactional
    public List<Attachment> uploadBatch(List<MultipartFile> files, Long userId) throws IOException {
        return uploadBatch(files, userId, null);
    }

    /**
     * 批量上传文件（带前缀）
     * 遍历文件列表，逐个上传
     *
     * @param files  文件列表
     * @param userId 用户ID
     * @param prefix 文件路径前缀
     * @return List<Attachment> 附件实体列表
     * @throws IOException 上传异常
     */
    @Override
    @Transactional
    public List<Attachment> uploadBatch(List<MultipartFile> files, Long userId, String prefix) throws IOException {
        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Attachment attachment = upload(file, userId, prefix);
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    /**
     * 根据ID获取附件信息
     *
     * @param id 附件ID
     * @return Attachment 附件实体
     */
    @Override
    public Attachment getById(Long id) {
        return attachmentMapper.selectById(id);
    }

    /**
     * 根据ID获取附件URL
     *
     * @param id 附件ID
     * @return 文件URL，不存在返回null
     */
    @Override
    public String getUrl(Long id) {
        Attachment attachment = getById(id);
        return attachment != null ? attachment.getFileUrl() : null;
    }

    /**
     * 删除附件
     * 删除OSS上的文件，并将数据库记录标记为已删除
     *
     * @param id 附件ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean delete(Long id) {
        Attachment attachment = getById(id);
        if (attachment == null) {
            return false;
        }

        ossUtil.delete(attachment.getFileUrl());

        attachment.setDeleteFlag(1);
        attachmentMapper.updateById(attachment);

        log.info("附件删除成功: id={}", id);
        return true;
    }

    /**
     * 批量删除附件
     *
     * @param ids 附件ID列表
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 获取文件后缀名
     *
     * @param fileName 文件名
     * @return 后缀名，不含点号
     */
    private String getFileSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 根据ID列表批量查询附件
     * 使用MyBatis-Plus的selectBatchIds方法实现IN查询
     *
     * @param idList 附件ID列表
     * @return 附件列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<Attachment> batchGetByIds(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return new ArrayList<>();
        }
        return attachmentMapper.selectBatchIds(idList);
    }
}
