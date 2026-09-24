package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 食物营养视图对象
 * 用于食物库列表和详情展示。
 */
@Data
@Builder
@Schema(description = "食物营养信息")
public class FoodVO {

    /** 食物ID */
    private Long id;

    /** 食物名称 */
    private String foodName;

    /** 食物分类 */
    private String category;

    /** 每100克热量 */
    private BigDecimal caloriesPer100g;

    /** 每100克蛋白质 */
    private BigDecimal proteinPer100g;

    /** 每100克脂肪 */
    private BigDecimal fatPer100g;

    /** 每100克碳水 */
    private BigDecimal carbsPer100g;

    /** 每100克膳食纤维 */
    private BigDecimal fiberPer100g;

    /** 可食部百分比 */
    private BigDecimal ediblePortion;

    /** 数据来源 */
    private String dataSource;
}
