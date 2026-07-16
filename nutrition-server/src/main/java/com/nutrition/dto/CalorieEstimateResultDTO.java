package com.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI热量估算结果DTO
 * 仅包含总热量数值，用于前端输入框回填
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI热量估算结果")
public class CalorieEstimateResultDTO {

    @Schema(description = "总热量（千卡），保留1位小数")
    private BigDecimal totalCalorie;
}