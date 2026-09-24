package com.nutrition.health;

import com.nutrition.dto.HealthCalorieSummaryDTO;
import com.nutrition.enums.HealthGoalTypeEnum;
import com.nutrition.util.HealthAnalysisPromptUtil;
import com.nutrition.vo.DailyCalorieVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 健康分析提示词测试。
 */
class HealthAnalysisPromptUtilTest {

    /** 验证四种目标生成不同提示词。 */
    @Test
    void shouldGenerateDifferentPromptForEachGoal() {
        List<DailyCalorieVO> days = List.of(
                DailyCalorieVO.builder().date(LocalDate.now()).calories(new BigDecimal("1800")).build()
        );
        HealthCalorieSummaryDTO summary = HealthCalorieSummaryDTO.builder()
                .last7Days(days)
                .last30Days(days)
                .last7Avg(new BigDecimal("1800.0"))
                .last30Avg(new BigDecimal("1800.0"))
                .last7Max(new BigDecimal("1800"))
                .last7Min(new BigDecimal("1800"))
                .recordedDaysIn30(1)
                .build();

        String fitness = HealthAnalysisPromptUtil.buildPrompt(HealthGoalTypeEnum.FITNESS, summary);
        String weightLoss = HealthAnalysisPromptUtil.buildPrompt(HealthGoalTypeEnum.WEIGHT_LOSS, summary);
        String normal = HealthAnalysisPromptUtil.buildPrompt(HealthGoalTypeEnum.NORMAL_DIET, summary);
        String gain = HealthAnalysisPromptUtil.buildPrompt(HealthGoalTypeEnum.WEIGHT_GAIN, summary);

        assertTrue(fitness.contains("健身"));
        assertTrue(weightLoss.contains("减肥"));
        assertTrue(normal.contains("正常饮食"));
        assertTrue(gain.contains("增重"));
        assertNotEquals(fitness, weightLoss);
    }
}
