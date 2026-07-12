package com.nutrition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nutrition.common.Result;
import com.nutrition.entity.UserFeedback;
import com.nutrition.param.FeedbackParam;
import com.nutrition.service.UserFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户问题反馈控制器
 */
@RestController
@RequestMapping("/user/feedback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户问题反馈", description = "用户问题反馈相关接口")
public class UserFeedbackController {

    private final UserFeedbackService userFeedbackService;

    @PostMapping("/submit")
    @Operation(summary = "提交问题反馈", description = "用户提交问题反馈，含内容安全审核流程")
    public Result<String> submitFeedback(HttpServletRequest request,
                                         @Valid @RequestBody FeedbackParam param) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到提交反馈请求: userId={}, contentLength={}", userId, param.getContent().length());

        userFeedbackService.submitFeedback(userId, param.getContent());

        return Result.ok("反馈提交成功");
    }

    @GetMapping("/list")
    @Operation(summary = "查询反馈列表", description = "查询当前用户的问题反馈历史列表，按创建时间倒序排列")
    public Result<List<UserFeedback>> getFeedbackList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到查询反馈列表请求: userId={}", userId);

        List<UserFeedback> list = userFeedbackService.getFeedbackList(userId);

        return Result.ok(list);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询反馈列表", description = "分页查询当前用户的问题反馈历史列表")
    public Result<IPage<UserFeedback>> getFeedbackPage(HttpServletRequest request,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("收到分页查询反馈列表请求: userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);

        Page<UserFeedback> page = new Page<>(pageNum, pageSize);
        IPage<UserFeedback> result = userFeedbackService.getFeedbackList(userId, page);

        return Result.ok(result);
    }
}