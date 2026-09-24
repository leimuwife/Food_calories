package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.common.BusinessException;
import com.nutrition.dto.DailyCalorieDTO;
import com.nutrition.dto.HealthCalorieSummaryDTO;
import com.nutrition.entity.HealthAnalysisReport;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.HealthAnalysisConfigEnum;
import com.nutrition.enums.HealthGoalTypeEnum;
import com.nutrition.enums.HealthReportStatusEnum;
import com.nutrition.mapper.HealthAnalysisReportMapper;
import com.nutrition.service.HealthAnalysisAiService;
import com.nutrition.service.HealthAnalysisService;
import com.nutrition.util.HealthAnalysisPromptUtil;
import com.nutrition.util.HealthAnalysisStatUtil;
import com.nutrition.vo.DailyCalorieVO;
import com.nutrition.vo.HealthAnalysisReportVO;
import com.nutrition.vo.HealthCalorieTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康分析业务实现。
 * 负责 MySQL 热量聚合、AI 报告重试、报告保存及历史报告降级。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthAnalysisServiceImpl implements HealthAnalysisService {

    /** 健康分析数据访问层 */
    private final HealthAnalysisReportMapper healthAnalysisReportMapper;

    /** 健康分析 AI 服务 */
    private final HealthAnalysisAiService healthAnalysisAiService;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 查询当前用户最近7天和30天热量趋势。
     *
     * @param userId 用户ID
     * @return 热量趋势
     */
    @Override
    @Transactional(readOnly = true)
    public HealthCalorieTrendVO getCalorieTrend(Long userId) {
        return HealthAnalysisStatUtil.toTrendVO(buildCalorieSummary(userId));
    }

    /**
     * 查询指定目标最近一次报告。
     *
     * @param userId 用户ID
     * @param goal   目标类型
     * @return 最近报告；不存在时返回 null
     */
    @Override
    @Transactional(readOnly = true)
    public HealthAnalysisReportVO getLatestReport(Long userId, HealthGoalTypeEnum goal) {
        validateRequest(userId, goal);
        HealthAnalysisReport report = findLatestReport(userId, goal);
        return report == null ? null : convertToVO(report, HealthReportStatusEnum.CURRENT);
    }

    /**
     * 生成并保存健康分析报告。
     * AI 连续失败3次后优先返回同目标最近报告。
     *
     * @param userId 用户ID
     * @param goal   目标类型
     * @return 新报告或历史降级报告
     */
    @Override
    @Transactional
    public HealthAnalysisReportVO generateReport(Long userId, HealthGoalTypeEnum goal) {
        validateRequest(userId, goal);
        HealthCalorieSummaryDTO summary = buildCalorieSummary(userId);
        String prompt = HealthAnalysisPromptUtil.buildPrompt(goal, summary);

        String reportContent;
        try {
            reportContent = executeAiWithRetry(prompt, userId);
        } catch (RuntimeException e) {
            log.error("健康分析AI连续失败: userId={}, goal={}, error={}", userId, goal.getCode(), e.getMessage(), e);
            HealthAnalysisReport historicalReport = findLatestReport(userId, goal);
            if (historicalReport == null) {
                throw new BusinessException(BizMsgEnum.HEALTH_AI_UNAVAILABLE);
            }
            return convertToVO(historicalReport, HealthReportStatusEnum.FALLBACK);
        }

        HealthAnalysisReport report = new HealthAnalysisReport();
        report.setUserId(userId);
        report.setGoalType(goal.getCode());
        report.setReportContent(reportContent);
        report.setCalorieSnapshot(serializeSnapshot(summary));
        report.setLast7Avg(summary.getLast7Avg());
        report.setLast30Avg(summary.getLast30Avg());
        report.setDeleteFlag(0);
        healthAnalysisReportMapper.insert(report);

        log.info("健康分析报告生成成功: userId={}, goal={}, reportId={}", userId, goal.getCode(), report.getId());
        return convertToVO(report, HealthReportStatusEnum.CURRENT);
    }

    /**
     * 构建热量统计摘要。
     *
     * @param userId 用户ID
     * @return 热量摘要
     */
    private HealthCalorieSummaryDTO buildCalorieSummary(Long userId) {
        if (userId == null) {
            throw new BusinessException(BizMsgEnum.USER_NOT_LOGIN);
        }
        LocalDate endDate = LocalDate.now();
        LocalDate start30Date = endDate.minusDays(HealthAnalysisConfigEnum.TREND_DAYS.getValue() - 1L);
        List<DailyCalorieDTO> records = healthAnalysisReportMapper.sumDailyCalories(userId, start30Date, endDate);
        List<DailyCalorieVO> last30Days = HealthAnalysisStatUtil.buildTrend(records, start30Date, endDate);
        int recentDays = HealthAnalysisConfigEnum.RECENT_DAYS.getValue();
        List<DailyCalorieVO> last7Days = new ArrayList<>(
                last30Days.subList(last30Days.size() - recentDays, last30Days.size()));
        return HealthAnalysisStatUtil.summarize(last7Days, last30Days);
    }

    /**
     * 按配置次数执行 AI 调用，并在失败时递增等待。
     *
     * @param prompt 分析提示词
     * @param userId 用户ID
     * @return AI 报告文本
     */
    private String executeAiWithRetry(String prompt, Long userId) {
        int maxAttempts = HealthAnalysisConfigEnum.MAX_AI_ATTEMPTS.getValue();
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String content = healthAnalysisAiService.generateReport(prompt, userId);
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException(BizMsgEnum.HEALTH_REPORT_EMPTY.getMessage());
                }
                return content.trim();
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("健康分析AI调用失败: attempt={}/{}, userId={}, error={}",
                        attempt, maxAttempts, userId, e.getMessage());
                if (attempt < maxAttempts) {
                    waitBeforeRetry(attempt);
                }
            }
        }
        throw lastException == null ? new IllegalStateException(BizMsgEnum.HEALTH_AI_UNAVAILABLE.getMessage()) : lastException;
    }

    /**
     * 重试前等待，避免连续快速失败。
     *
     * @param attempt 当前尝试次数
     */
    private void waitBeforeRetry(int attempt) {
        long delay = (long) HealthAnalysisConfigEnum.RETRY_BASE_DELAY_MILLIS.getValue() * attempt;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(BizMsgEnum.HEALTH_AI_UNAVAILABLE.getMessage(), e);
        }
    }

    /**
     * 查询同目标最近报告。
     *
     * @param userId 用户ID
     * @param goal   目标类型
     * @return 最近报告或 null
     */
    private HealthAnalysisReport findLatestReport(Long userId, HealthGoalTypeEnum goal) {
        return healthAnalysisReportMapper.selectOne(
                new LambdaQueryWrapper<HealthAnalysisReport>()
                        .eq(HealthAnalysisReport::getUserId, userId)
                        .eq(HealthAnalysisReport::getGoalType, goal.getCode())
                        .orderByDesc(HealthAnalysisReport::getCreateTime)
                        .orderByDesc(HealthAnalysisReport::getId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 序列化热量快照。
     *
     * @param summary 热量摘要
     * @return JSON 文本
     */
    private String serializeSnapshot(HealthCalorieSummaryDTO summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.error("健康分析热量快照序列化失败: error={}", e.getMessage(), e);
            throw new BusinessException(BizMsgEnum.HEALTH_REPORT_SAVE_FAILED);
        }
    }

    /**
     * 转换报告实体为响应对象。
     *
     * @param report 报告实体
     * @param status 报告状态
     * @return 报告响应
     */
    private HealthAnalysisReportVO convertToVO(HealthAnalysisReport report, HealthReportStatusEnum status) {
        HealthGoalTypeEnum goal = HealthGoalTypeEnum.fromCode(report.getGoalType());
        return HealthAnalysisReportVO.builder()
                .reportId(report.getId())
                .goalType(goal)
                .goalLabel(goal == null ? report.getGoalType() : goal.getLabel())
                .reportContent(report.getReportContent())
                .reportStatus(status)
                .last7Avg(report.getLast7Avg())
                .last30Avg(report.getLast30Avg())
                .generatedTime(report.getCreateTime())
                .build();
    }

    /**
     * 校验用户和目标参数。
     *
     * @param userId 用户ID
     * @param goal   目标类型
     */
    private void validateRequest(Long userId, HealthGoalTypeEnum goal) {
        if (userId == null) {
            throw new BusinessException(BizMsgEnum.USER_NOT_LOGIN);
        }
        if (goal == null) {
            throw new BusinessException(BizMsgEnum.HEALTH_GOAL_INVALID);
        }
    }
}
