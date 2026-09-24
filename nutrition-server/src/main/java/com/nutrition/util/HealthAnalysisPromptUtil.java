package com.nutrition.util;

import com.nutrition.dto.HealthCalorieSummaryDTO;
import com.nutrition.enums.HealthGoalTypeEnum;
import lombok.experimental.UtilityClass;

import java.util.stream.Collectors;

/**
 * 健康分析提示词工具类。
 * 根据目标类型和热量摘要生成差异化 AI 分析要求。
 */
@UtilityClass
public class HealthAnalysisPromptUtil {

    /**
     * 构建健康分析报告提示词。
     *
     * @param goal    用户目标类型
     * @param summary 热量统计摘要
     * @return 完整 AI 提示词
     */
    public String buildPrompt(HealthGoalTypeEnum goal, HealthCalorieSummaryDTO summary) {
        String dailyData = summary.getLast30Days().stream()
                .map(item -> item.getDate() + ":" + item.getCalories() + "kcal")
                .collect(Collectors.joining("，"));

        return """
                你是一名谨慎、专业的营养分析助手。请根据用户的历史热量记录生成中文健康分析报告。

                用户目标：%s
                目标要求：%s

                最近7天平均热量：%s kcal
                最近30天平均热量：%s kcal
                最近7天最高热量：%s kcal
                最近7天最低热量：%s kcal
                最近30天有记录天数：%d 天
                最近30天每日热量：%s

                请严格按照以下结构输出：
                1. 数据概览：说明摄入趋势、波动和记录完整度。
                2. 目标匹配分析：结合“%s”目标判断当前饮食是否合理。
                3. 饮食建议：给出3到5条可执行建议。
                4. 注意事项：提醒数据缺失、极端值或需要专业医生介入的情况。

                要求：只基于给定数据推断，不编造疾病诊断，不承诺减重或增重效果。
                """.formatted(
                goal.getLabel(),
                goal.getPromptDirection(),
                summary.getLast7Avg(),
                summary.getLast30Avg(),
                summary.getLast7Max(),
                summary.getLast7Min(),
                summary.getRecordedDaysIn30(),
                dailyData,
                goal.getLabel()
        );
    }
}
