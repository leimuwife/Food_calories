package com.nutrition.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 每日饮食汇总视图对象
 * 用于返回用户每日营养摄入汇总数据
 */
@Data
public class DailySummaryVO {

    private Integer totalCalories;

    private Double totalProtein;

    private Double totalFat;

    private Double totalCarbs;

    private Integer calorieGoal;

    private Integer proteinGoal;

    private Integer fatGoal;

    private Integer carbsGoal;

    private Integer caloriePercent;

    private Integer proteinPercent;

    private Integer fatPercent;

    private Integer carbsPercent;

    private Double proteinRatio;

    private Double fatRatio;

    private Double carbsRatio;

    private Map<String, MealSummaryVO> meals;

    @Data
    public static class MealSummaryVO {
        private Integer calories;
        private Double protein;
        private Double fat;
        private Double carbs;
        private List<DietItemVO> items;
    }
}
