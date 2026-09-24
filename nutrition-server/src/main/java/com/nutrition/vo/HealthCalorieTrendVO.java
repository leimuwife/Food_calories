package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 健康分析热量趋势视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "健康分析热量趋势")
public class HealthCalorieTrendVO {

    /** 最近7天热量 */
    @Schema(description = "最近7天每日热量")
    private List<DailyCalorieVO> last7Days;

    /** 最近30天热量 */
    @Schema(description = "最近30天每日热量")
    private List<DailyCalorieVO> last30Days;
}
