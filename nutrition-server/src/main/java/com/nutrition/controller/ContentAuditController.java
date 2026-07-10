package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.ContentAuditRecord;
import com.nutrition.service.ContentAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容审核控制器
 * 提供审核记录查询、人工复审等管理接口
 */
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Slf4j
public class ContentAuditController {

    private final ContentAuditService contentAuditService;

    /**
     * 查询待复审的审核记录
     * GET /api/audit/pending-review
     *
     * @return 待复审记录列表
     */
    @GetMapping("/pending-review")
    public Result<List<ContentAuditRecord>> getPendingReviewRecords() {
        List<ContentAuditRecord> records = contentAuditService.getPendingReviewRecords();
        return Result.ok(records);
    }

    /**
     * 根据用户ID查询审核记录
     * GET /api/audit/user/{userId}
     *
     * @param userId 用户ID
     * @return 用户的审核记录列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<ContentAuditRecord>> getRecordsByUserId(@PathVariable("userId") Long userId) {
        List<ContentAuditRecord> records = contentAuditService.getRecordsByUserId(userId);
        return Result.ok(records);
    }

    /**
     * 处理人工复审
     * POST /api/audit/review
     *
     * @param recordId     审核记录ID
     * @param reviewResult 复审结论：0合规 1确认违规
     * @param reviewRemark 复审备注
     * @return 操作结果
     */
    @PostMapping("/review")
    public Result<Void> processReview(
            @RequestParam("recordId") Long recordId,
            @RequestParam("reviewResult") Integer reviewResult,
            @RequestParam(value = "reviewRemark", required = false) String reviewRemark) {

        contentAuditService.processReview(recordId, reviewResult, reviewRemark);
        return Result.ok("复审完成", null);
    }
}
