package com.nutrition.dto;

import com.nutrition.vo.DailyCalorieVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 健康分析热量摘要。
 * 用于向前端展示和构造 AI 分析 Prompt。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCalorieSummaryDTO {

    /** 最近7天每日热量 */
    private List<DailyCalorieVO> last7Days;

    /** 最近30天每日热量 */
    private List<DailyCalorieVO> last30Days;

    /** 最近7天平均热量 */
    private BigDecimal last7Avg;

    /** 最近30天平均热量 */
    private BigDecimal last30Avg;

    /** 最近7天最高热量 */
    private BigDecimal last7Max;

    /** 最近7天最低热量 */
    private BigDecimal last7Min;

    /** 最近30天有记录的天数 */
    private int recordedDaysIn30;
}
