package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.RagDocumentStatusEnum;
import com.nutrition.service.RagKnowledgeService;
import com.nutrition.vo.KnowledgeCallbackVO;
import com.nutrition.vo.KnowledgeDocumentVO;
import com.nutrition.vo.KnowledgeUploadVO;
import com.nutrition.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * RAG知识库文档管理控制器
 * 提供知识库文档上传、查询、删除接口
 */
@RestController
@RequestMapping("/rag/knowledge")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RAG知识库管理", description = "知识库文档上传、查询、删除")
public class RagKnowledgeController {

    private final RagKnowledgeService ragKnowledgeService;

    /**
     * 上传知识库文档
     * 流程：MD5计算→MySQL查重→保存记录→调用Python入库
     *
     * @param request HTTP请求（获取当前用户）
     * @param file 上传的文档文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    @Operation(summary = "上传知识库文档", description = "上传文档至RAG知识库，支持pdf/txt/md/docx/json/jsonl等格式")
    public Result<KnowledgeUploadVO> uploadDocument(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {

        Long userId = (Long) request.getAttribute("userId");
        String originalFilename = file.getOriginalFilename();

        log.info("知识库文档上传请求: fileName={}, userId={}, size={}",
                originalFilename, userId, file.getSize());

        if (file.isEmpty()) {
            return Result.fail(BizMsgEnum.RAG_FILE_EMPTY.getMessage());
        }

        try {
            // Service 直接返回 KnowledgeUploadVO（success=true 正常成功；duplicate=true 文件重复）
            // 两者 code 均为 200，避免 axios 拦截器误判 duplicate 为失败
            KnowledgeUploadVO uploadVo = ragKnowledgeService.uploadDocument(file, userId);

            if (Boolean.TRUE.equals(uploadVo.getDuplicate())) {
                return Result.ok(BizMsgEnum.RAG_FILE_DUPLICATE.getMessage(), uploadVo);
            }
            return Result.ok("文档上传成功，正在进行向量入库", uploadVo);
        } catch (Exception e) {
            log.error("{}: {}", BizMsgEnum.RAG_UPLOAD_FAILED.getMessage(), e.getMessage(), e);
            return Result.fail(BizMsgEnum.RAG_UPLOAD_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * 查询知识库文档列表
     *
     * @param keyword 关键词（按文档名称模糊搜索）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "查询知识库文档列表", description = "分页查询已上传的知识库文档")
    public Result<PageVO<KnowledgeDocumentVO>> listDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        log.info("查询知识库列表: keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);

        PageVO<KnowledgeDocumentVO> pageVO = ragKnowledgeService.listDocuments(keyword, pageNum, pageSize);
        return Result.ok(pageVO);
    }

    /**
     * 删除知识库文档
     *
     * @param id 文档ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库文档", description = "删除文档及对应的向量库数据")
    public Result<Void> deleteDocument(@PathVariable("id") Long id) {
        log.info("删除知识库文档: docId={}", id);

        try {
            ragKnowledgeService.deleteDocument(id);
            return Result.ok("文档删除成功", null);
        } catch (Exception e) {
            log.error("{}: docId={}, error={}", BizMsgEnum.RAG_DELETE_FAILED.getMessage(), id, e.getMessage(), e);
            return Result.fail(BizMsgEnum.RAG_DELETE_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * Python AI服务回调接口
     * 用于Python完成向量入库后通知Java更新文档状态
     *
     * @param request 回调请求体
     * @return 处理结果
     */
    @PostMapping("/callback")
    @Operation(summary = "Python回调接口", description = "Python AI服务向量入库完成后的状态回调")
    public Result<KnowledgeCallbackVO> callback(@RequestBody Map<String, Object> request) {
        log.info("收到Python回调: {}", request);

        try {
            String docId = (String) request.get("doc_id");
            String status = (String) request.get("status");
            String message = (String) request.get("message");
            String collectionId = (String) request.get("collection_id");

            if (docId == null) {
                return Result.fail(BizMsgEnum.RAG_CALLBACK_DOC_ID_EMPTY.getMessage());
            }

            int statusCode;
            switch (status) {
                case "success":
                    statusCode = RagDocumentStatusEnum.NORMAL.getCode();
                    break;
                case "failed":
                    statusCode = RagDocumentStatusEnum.FAILED.getCode();
                    break;
                default:
                    statusCode = RagDocumentStatusEnum.FAILED.getCode();
                    break;
            }

            ragKnowledgeService.updateDocumentStatus(
                    Long.parseLong(docId), statusCode, collectionId, message
            );

            return Result.ok("回调处理成功", new KnowledgeCallbackVO(true, docId));
        } catch (Exception e) {
            log.error("{}: {}", BizMsgEnum.RAG_CALLBACK_PROCESS_FAILED.getMessage(), e.getMessage(), e);
            return Result.fail(BizMsgEnum.RAG_CALLBACK_PROCESS_FAILED.getMessage() + ": " + e.getMessage());
        }
    }
}
