package com.nutrition.vo;

import lombok.Data;

/**
 * 食物信息视图对象
 * 用于返回食物字典的详细信息
 */
@Data
public class FoodVO {

    private Long id;

    private String foodName;

    private String category;

    private Integer caloriesPer100g;

    private Double proteinPer100g;

    private Double fatPer100g;

    private Double carbsPer100g;

    private Double fiberPer100g;

    private Double ediblePortion;

    private String dataSource;
}
