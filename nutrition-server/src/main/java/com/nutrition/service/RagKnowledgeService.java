package com.nutrition.service;

import com.nutrition.entity.RagKnowledgeDocument;
import com.nutrition.vo.KnowledgeDocumentVO;
import com.nutrition.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG知识库文档管理服务
 */
public interface RagKnowledgeService {

    /**
     * 文档上传（完整流程：MD5计算→查重→保存→调用Python入库）
     *
     * @param file 上传文件
     * @param userId 上传管理员ID
     * @return 上传结果
     */
    KnowledgeDocumentVO uploadDocument(MultipartFile file, Long userId);

    /**
     * 分页查询文档列表
     *
     * @param keyword 关键词（文档名称模糊搜索）
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<KnowledgeDocumentVO> listDocuments(String keyword, int pageNum, int pageSize);

    /**
     * 删除文档（同时调用Python删除向量）
     *
     * @param id 文档ID
     */
    void deleteDocument(Long id);

    /**
     * Python回调：更新文档入库状态
     *
     * @param docId 文档ID
     * @param status 状态
     * @param vectorStoreId 向量库ID
     * @param message 消息
     */
    void updateDocumentStatus(Long docId, Integer status, String vectorStoreId, String message);
}
