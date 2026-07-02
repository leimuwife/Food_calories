package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 食物字典表实体
 */
@Data
@TableName("food_dict")
public class FoodDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食物名称 */
    private String foodName;

    /** 食物分类 */
    private String category;

    /** 每100g热量(kcal) */
    private Integer caloriesPer100g;

    /** 每100g蛋白质(g) */
    private Double proteinPer100g;

    /** 每100g脂肪(g) */
    private Double fatPer100g;

    /** 每100g碳水化合物(g) */
    private Double carbsPer100g;

    /** 每100g膳食纤维(g) */
    private Double fiberPer100g;

    /** 可食部比例(%) */
    private Double ediblePortion;

    /** 数据来源 */
    private String dataSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
