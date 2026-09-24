package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 健康分析报告实体
 * 对应数据库表 health_analysis_report，保存用户 AI 饮食分析结果。
 */
@Data
@TableName("health_analysis_report")
public class HealthAnalysisReport extends Common {

    /** 报告主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "报告主键ID")
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    /** 目标类型编码，取值由 HealthGoalTypeEnum 管理 */
    @TableField("goal_type")
    @Schema(description = "目标类型编码")
    private String goalType;

    /** AI 报告正文 */
    @TableField("report_content")
    @Schema(description = "AI报告正文")
    private String reportContent;

    /** 生成报告时的热量快照 JSON */
    @TableField("calorie_snapshot")
    @Schema(description = "热量数据快照JSON")
    private String calorieSnapshot;

    /** 最近7天日均热量 */
    @TableField("last_7_avg")
    @Schema(description = "最近7天日均热量")
    private BigDecimal last7Avg;

    /** 最近30天日均热量 */
    @TableField("last_30_avg")
    @Schema(description = "最近30天日均热量")
    private BigDecimal last30Avg;
}
