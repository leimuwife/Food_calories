package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 食物营养数据实体类
 * 对应数据库表 food_nutrition
 * 存储食物的营养成分信息，供AI热量估算和用户查询使用
 */
@Data
@TableName("food_nutrition")
public class FoodNutrition extends Common {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("food_name")
    @Schema(description = "食物名称")
    private String foodName;

    @TableField("food_category")
    @Schema(description = "食物分类")
    private String foodCategory;

    @TableField("edible_part")
    @Schema(description = "可食部百分比(%)")
    private BigDecimal ediblePart;

    @TableField("calorie")
    @Schema(description = "能量(千卡/100g)")
    private BigDecimal calorie;

    @TableField("protein")
    @Schema(description = "蛋白质(克/100g)")
    private BigDecimal protein;

    @TableField("fat")
    @Schema(description = "脂肪(克/100g)")
    private BigDecimal fat;

    @TableField("carbohydrate")
    @Schema(description = "碳水化合物(克/100g)")
    private BigDecimal carbohydrate;
}