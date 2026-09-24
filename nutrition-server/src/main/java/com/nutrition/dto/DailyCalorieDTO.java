package com.nutrition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日热量数据库聚合结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCalorieDTO {

    /** 日期 */
    private LocalDate date;

    /** 当日总热量 */
    private BigDecimal calories;
}
