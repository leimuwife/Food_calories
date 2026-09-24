package com.nutrition.vo;

import com.nutrition.enums.HealthGoalTypeEnum;
import com.nutrition.enums.HealthReportStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康分析报告视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "健康分析报告")
public class HealthAnalysisReportVO {

    /** 报告ID */
    private Long reportId;

    /** 目标类型 */
    private HealthGoalTypeEnum goalType;

    /** 目标中文名称 */
    private String goalLabel;

    /** 报告正文 */
    private String reportContent;

    /** 报告状态 */
    private HealthReportStatusEnum reportStatus;

    /** 最近7天平均热量 */
    private BigDecimal last7Avg;

    /** 最近30天平均热量 */
    private BigDecimal last30Avg;

    /** 报告生成时间 */
    private LocalDateTime generatedTime;
}
