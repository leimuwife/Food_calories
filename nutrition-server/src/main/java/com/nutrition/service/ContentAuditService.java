package com.nutrition.service;

import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.entity.ContentAuditRecord;

import java.util.List;

/**
 * 内容审核服务接口
 * 提供文本审核、图片审核等核心能力
 */
public interface ContentAuditService {

    /**
     * 审核文本内容
     * 调用微信 msgSecCheck 接口进行文本安全审核
     *
     * @param userId     用户ID
     * @param openid     用户微信openid
     * @param content    待审核文本内容
     * @param scene      业务场景
     * @return 审核结果建议
     */
    AuditSuggestEnum auditText(Long userId, String openid, String content, AuditSceneEnum scene);

    /**
     * 审核图片内容
     * 调用微信 imgSecCheck 接口进行图片安全审核
     * 支持批量审核，任意一张违规则整体拦截
     *
     * @param userId   用户ID
     * @param openid   用户微信openid
     * @param fileIds  待审核图片附件ID列表（附件表主键ID）
     * @param scene    业务场景
     * @return 审核结果建议
     */
    AuditSuggestEnum auditImages(Long userId, String openid, List<String> fileIds, AuditSceneEnum scene);

    /**
     * 获取待复审的审核记录
     *
     * @return 待复审记录列表
     */
    List<ContentAuditRecord> getPendingReviewRecords();

    /**
     * 根据用户ID查询审核记录
     *
     * @param userId 用户ID
     * @return 审核记录列表
     */
    List<ContentAuditRecord> getRecordsByUserId(Long userId);

    /**
     * 保存审核记录
     *
     * @param record 审核记录
     */
    void saveAuditRecord(ContentAuditRecord record);

    /**
     * 处理人工复审
     * 保留逻辑，后续开发
     *
     * @param recordId      审核记录ID
     * @param reviewResult  复审结论：0合规 1确认违规
     * @param reviewRemark  复审备注
     */
    void processReview(Long recordId, Integer reviewResult, String reviewRemark);
}
