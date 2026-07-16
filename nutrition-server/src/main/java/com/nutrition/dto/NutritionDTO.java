package com.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 食物营养数据传输对象
 * 仅包含AI热量估算所需的4个核心数值字段，减少冗余数据传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "食物营养精简数据")
public class NutritionDTO {

    @Schema(description = "热量(kcal/100g)")
    private BigDecimal calorie;

    @Schema(description = "蛋白质(g/100g)")
    private BigDecimal protein;

    @Schema(description = "脂肪(g/100g)")
    private BigDecimal fat;

    @Schema(description = "碳水化合物(g/100g)")
    private BigDecimal carbohydrate;
}