package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单日热量视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单日热量")
public class DailyCalorieVO {

    /** 日期 */
    @Schema(description = "日期")
    private LocalDate date;

    /** 当日摄入热量 */
    @Schema(description = "当日摄入热量(kcal)")
    private BigDecimal calories;
}
