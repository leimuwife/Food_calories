package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 饮食记录视图对象
 * 用于返回单条饮食记录信息
 */
@Data
public class DietRecordVO {

    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "记录日期")
    private String recordDate;

    @Schema(description = "餐次类型")
    private String mealType;

    @Schema(description = "餐次名称")
    private String mealTypeName;

    @Schema(description = "餐次总热量")
    private java.math.BigDecimal totalCalories;

    @Schema(description = "饮食项列表")
    private List<DietItemVO> items;
}