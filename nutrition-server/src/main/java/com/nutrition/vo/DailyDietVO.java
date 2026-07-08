package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 每日饮食视图对象
 * 用于返回当日完整饮食数据
 */
@Data
public class DailyDietVO {

    @Schema(description = "今日总热量")
    private BigDecimal totalCalories;

    @Schema(description = "早餐热量")
    private BigDecimal breakfastCalories;

    @Schema(description = "午餐热量")
    private BigDecimal lunchCalories;

    @Schema(description = "晚餐热量")
    private BigDecimal dinnerCalories;

    @Schema(description = "夜宵热量")
    private BigDecimal snackCalories;

    @Schema(description = "饮食记录列表")
    private List<DietRecordVO> records;

    @Schema(description = "食物列表（扁平化展示）")
    private List<DietItemVO> foodList;
}