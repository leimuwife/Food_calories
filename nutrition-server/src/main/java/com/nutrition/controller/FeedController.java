package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.param.FeedCommentParam;
import com.nutrition.param.FeedListParam;
import com.nutrition.param.FeedPublishParam;
import com.nutrition.service.FeedService;
import com.nutrition.vo.FeedListResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 轻友圈动态控制器
 * 提供动态发布、查询、点赞、评论等接口
 */
@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "轻友圈动态", description = "动态发布、查询、点赞、评论相关接口")
public class FeedController {

    private final FeedService feedService;

    /**
     * 发布动态
     * 参数校验：content和fileIds均不能为空（通过@Valid自动校验）
     * 审核流程：文本审核 → 图片审核 → 保存动态
     *
     * @param request HTTP请求对象，用于获取当前登录用户ID
     * @param param   发布动态请求参数（已通过 @Valid 自动校验）
     * @return 动态ID
     */
    @PostMapping("/publish")
    @Operation(summary = "发布动态", description = "用户发布动态，包含文本和图片审核流程")
    public Result<Map<String, Object>> publishFeed(HttpServletRequest request,
                                                   @Valid @RequestBody FeedPublishParam param) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到发布动态请求: userId={}", userId);

        Long feedId = feedService.publishFeed(userId, param);

        Map<String, Object> data = new HashMap<>();
        data.put("feedId", feedId);

        return Result.ok("发布成功", data);
    }

    /**
     * 点赞/取消点赞动态
     * 逻辑：SETNX防重 → INCR/DECR计数 → 插入/删除明细记录
     * 不实时更新MySQL动态表，仅操作Redis和明细表
     *
     * @param request HTTP请求对象，用于获取当前登录用户ID
     * @param feedId  动态ID（路径参数）
     * @return 操作结果：isLiked（是否已点赞）、likeCount（当前点赞数）
     */
    @PostMapping("/{feedId}/like")
    @Operation(summary = "点赞/取消点赞", description = "用户对动态进行点赞或取消点赞操作")
    public Result<Map<String, Object>> toggleLike(HttpServletRequest request,
                                                  @PathVariable Long feedId) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到点赞请求: userId={}, feedId={}", userId, feedId);

        Map<String, Object> result = feedService.toggleLike(userId, feedId);

        return Result.ok("操作成功", result);
    }

    /**
     * 添加评论
     * 参数校验：content不能为空（通过@Valid自动校验）
     * 审核流程：文本审核 → 插入评论明细 → INCR计数
     * 不实时更新MySQL动态表，仅操作Redis和明细表
     *
     * @param request HTTP请求对象，用于获取当前登录用户ID
     * @param feedId  动态ID（路径参数）
     * @param param   评论请求参数（已通过 @Valid 自动校验）
     * @return 操作结果：commentCount（当前评论数）
     */
    @PostMapping("/{feedId}/comment")
    @Operation(summary = "添加评论", description = "用户对动态添加评论，包含文本审核流程")
    public Result<Map<String, Object>> addComment(HttpServletRequest request,
                                                  @PathVariable Long feedId,
                                                  @Valid @RequestBody FeedCommentParam param) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到评论请求: userId={}, feedId={}, contentLength={}", userId, feedId, param.getContent().length());

        Map<String, Object> result = feedService.addComment(userId, feedId, param);

        return Result.ok("评论成功", result);
    }

    /**
     * 获取动态列表（分页）
     * 参数校验：pageNum和pageSize均不能为空，且必须大于等于1（通过@Valid自动校验）
     * 数据来源：MySQL分页查询动态主表 + Redis获取实时计数 + Redis判断点赞状态
     *
     * @param request HTTP请求对象，用于获取当前登录用户ID
     * @param param   分页请求参数（已通过 @Valid 自动校验）
     * @return 动态列表查询结果
     */
    @GetMapping("/list")
    @Operation(summary = "获取动态列表", description = "分页获取轻友圈动态列表，包含动态详情、点赞状态、评论列表")
    public Result<FeedListResultVO> getFeedList(HttpServletRequest request,
                                                @Valid FeedListParam param) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到获取动态列表请求: userId={}, pageNum={}, pageSize={}", userId, param.getPageNum(), param.getPageSize());

        FeedListResultVO result = feedService.queryFeedList(userId, param);

        return Result.ok("success", result);
    }
}
