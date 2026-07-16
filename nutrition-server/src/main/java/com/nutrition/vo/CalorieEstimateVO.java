package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI热量估算返回VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI热量估算返回")
public class CalorieEstimateVO {

    @Schema(description = "总热量（千卡），保留1位小数")
    private BigDecimal totalCalorie;
}