package com.nutrition.vo;

import lombok.Data;
import java.util.List;

/**
 * 月度统计汇总视图对象
 * 用于返回用户月度营养摄入统计数据
 */
@Data
public class MonthlySummaryVO {

    private Integer avgDailyCalories;

    private Double avgProteinRatio;

    private Double avgFatRatio;

    private Double avgCarbsRatio;

    private List<DailyTrendVO> dailyTrend;

    private List<TopFoodVO> topFoods;

    private Integer totalDays;

    @Data
    public static class DailyTrendVO {
        private Integer day;
        private Integer calories;
    }

    @Data
    public static class TopFoodVO {
        private String foodName;
        private Integer count;
    }
}
