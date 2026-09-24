package com.nutrition.util;

import com.nutrition.dto.DailyCalorieDTO;
import com.nutrition.dto.HealthCalorieSummaryDTO;
import com.nutrition.vo.DailyCalorieVO;
import com.nutrition.vo.HealthCalorieTrendVO;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 健康分析统计工具类。
 * 负责日期补零、日均和极值计算。
 */
@UtilityClass
public class HealthAnalysisStatUtil {

    /**
     * 构建连续日期的热量趋势。
     *
     * @param records   数据库聚合结果
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 连续且升序的热量列表
     */
    public List<DailyCalorieVO> buildTrend(List<DailyCalorieDTO> records,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        Map<LocalDate, BigDecimal> calorieMap = records == null
                ? Map.of()
                : records.stream().collect(Collectors.toMap(
                        DailyCalorieDTO::getDate,
                        item -> defaultDecimal(item.getCalories()),
                        BigDecimal::add
                ));
        List<DailyCalorieVO> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(DailyCalorieVO.builder()
                    .date(date)
                    .calories(calorieMap.getOrDefault(date, BigDecimal.ZERO))
                    .build());
        }
        return result;
    }

    /**
     * 汇总最近7天和最近30天热量。
     *
     * @param last7Days 最近7天数据
     * @param last30Days 最近30天数据
     * @return 热量摘要
     */
    public HealthCalorieSummaryDTO summarize(List<DailyCalorieVO> last7Days,
                                             List<DailyCalorieVO> last30Days) {
        return HealthCalorieSummaryDTO.builder()
                .last7Days(last7Days)
                .last30Days(last30Days)
                .last7Avg(average(last7Days))
                .last30Avg(average(last30Days))
                .last7Max(maximum(last7Days))
                .last7Min(minimum(last7Days))
                .recordedDaysIn30((int) last30Days.stream()
                        .filter(item -> item.getCalories().compareTo(BigDecimal.ZERO) > 0)
                        .count())
                .build();
    }

    /**
     * 将摘要转换为接口响应。
     *
     * @param summary 热量摘要
     * @return 趋势响应
     */
    public HealthCalorieTrendVO toTrendVO(HealthCalorieSummaryDTO summary) {
        return HealthCalorieTrendVO.builder()
                .last7Days(summary.getLast7Days())
                .last30Days(summary.getLast30Days())
                .build();
    }

    /**
     * 计算平均热量。
     *
     * @param items 每日热量
     * @return 保留1位小数的平均值
     */
    public BigDecimal average(List<DailyCalorieVO> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal total = items.stream()
                .map(item -> defaultDecimal(item.getCalories()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(items.size()), 1, RoundingMode.HALF_UP);
    }

    /**
     * 查询最高热量。
     *
     * @param items 每日热量
     * @return 最高值
     */
    public BigDecimal maximum(List<DailyCalorieVO> items) {
        return items == null || items.isEmpty() ? BigDecimal.ZERO
                : items.stream().map(DailyCalorieVO::getCalories).max(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO);
    }

    /**
     * 查询最低热量。
     *
     * @param items 每日热量
     * @return 最低值
     */
    public BigDecimal minimum(List<DailyCalorieVO> items) {
        return items == null || items.isEmpty() ? BigDecimal.ZERO
                : items.stream().map(DailyCalorieVO::getCalories).min(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO);
    }

    /**
     * 将空值转换为零。
     *
     * @param value 原始值
     * @return 非空值
     */
    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
