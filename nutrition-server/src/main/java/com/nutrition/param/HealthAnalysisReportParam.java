package com.nutrition.param;

import com.nutrition.enums.HealthGoalTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 健康分析报告生成请求参数。
 */
@Data
@Schema(description = "健康分析报告生成参数")
public class HealthAnalysisReportParam {

    /** 用户目标类型 */
    @NotNull(message = "健康分析目标类型不能为空")
    @Schema(description = "目标类型：fitness/weight_loss/normal_diet/weight_gain")
    private HealthGoalTypeEnum goalType;
}