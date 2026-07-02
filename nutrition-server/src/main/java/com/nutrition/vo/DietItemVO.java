package com.nutrition.vo;

import lombok.Data;

/**
 * 饮食明细视图对象
 * 用于返回饮食记录中食物明细的详细信息
 */
@Data
public class DietItemVO {

    private Long id;

    private Long foodId;

    private String foodName;

    private Integer weight;

    private Integer calories;

    private Double protein;

    private Double fat;

    private Double carbs;
}
