package com.nutrition.health;

import com.nutrition.dto.HealthCalorieSummaryDTO;
import com.nutrition.util.HealthAnalysisStatUtil;
import com.nutrition.vo.DailyCalorieVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 健康分析统计工具测试。
 */
class HealthAnalysisStatUtilTest {

    /** 验证趋势补零、长度和平均值。 */
    @Test
    void shouldFillMissingDatesAndCalculateAverage() {
        LocalDate end = LocalDate.of(2026, 9, 24);
        var trend = HealthAnalysisStatUtil.buildTrend(List.of(), end.minusDays(6), end);
        assertEquals(7, trend.size());
        assertEquals(0, trend.get(0).getCalories().compareTo(BigDecimal.ZERO));

        List<DailyCalorieVO> values = List.of(
                DailyCalorieVO.builder().date(end).calories(new BigDecimal("1000")).build(),
                DailyCalorieVO.builder().date(end.minusDays(1)).calories(new BigDecimal("2000")).build()
        );
        HealthCalorieSummaryDTO summary = HealthAnalysisStatUtil.summarize(values, values);
        assertEquals(0, summary.getLast7Avg().compareTo(new BigDecimal("1500.0")));
        assertEquals(2, summary.getRecordedDaysIn30());
    }
}
