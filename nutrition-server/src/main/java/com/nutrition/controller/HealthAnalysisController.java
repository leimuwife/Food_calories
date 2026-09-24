package com.nutrition.controller;

import com.nutrition.common.BusinessException;
import com.nutrition.common.Result;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.HealthGoalTypeEnum;
import com.nutrition.param.HealthAnalysisReportParam;
import com.nutrition.service.HealthAnalysisService;
import com.nutrition.vo.HealthAnalysisReportVO;
import com.nutrition.vo.HealthCalorieTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康分析控制器。
 * 提供热量趋势、最近报告和 AI 报告生成接口。
 */
@RestController
@RequestMapping("/health-analysis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "健康分析", description = "热量趋势和AI饮食分析报告")
public class HealthAnalysisController {

    /** 健康分析业务服务 */
    private final HealthAnalysisService healthAnalysisService;

    /**
     * 查询当前用户最近7天和30天热量趋势。
     *
     * @param request HTTP 请求，用户ID从 JWT 属性获取
     * @return 热量趋势
     */
    @GetMapping("/calories")
    @Operation(summary = "查询热量趋势", description = "查询最近7天和30天每日热量")
    public Result<HealthCalorieTrendVO> getCalorieTrend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized(BizMsgEnum.USER_NOT_LOGIN.getMessage());
        }
        return Result.ok(healthAnalysisService.getCalorieTrend(userId));
    }

    /**
     * 查询指定目标最近一次报告。
     *
     * @param goalType 目标类型编码
     * @param request  HTTP 请求，用户ID从 JWT 属性获取
     * @return 最近报告；不存在时 data 为 null
     */
    @GetMapping("/report/latest")
    @Operation(summary = "查询最近报告", description = "按目标类型查询最近一次保存报告，不调用AI")
    public Result<HealthAnalysisReportVO> getLatestReport(@RequestParam("goalType") String goalType,
                                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized(BizMsgEnum.USER_NOT_LOGIN.getMessage());
        }
        HealthGoalTypeEnum goal = HealthGoalTypeEnum.fromCode(goalType);
        if (goal == null) {
            throw new BusinessException(BizMsgEnum.HEALTH_GOAL_INVALID);
        }
        return Result.ok(healthAnalysisService.getLatestReport(userId, goal));
    }

    /**
     * 生成并保存健康分析报告。
     *
     * @param param   报告生成参数
     * @param request HTTP 请求，用户ID从 JWT 属性获取
     * @return 新报告或同目标历史降级报告
     */
    @PostMapping("/report")
    @Operation(summary = "生成健康分析报告", description = "根据目标类型和热量历史生成并保存AI报告")
    public Result<HealthAnalysisReportVO> generateReport(@Valid @RequestBody HealthAnalysisReportParam param,
                                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized(BizMsgEnum.USER_NOT_LOGIN.getMessage());
        }
        if (param.getGoalType() == null) {
            throw new BusinessException(BizMsgEnum.HEALTH_GOAL_INVALID);
        }
        return Result.ok(healthAnalysisService.generateReport(userId, param.getGoalType()));
    }
}
