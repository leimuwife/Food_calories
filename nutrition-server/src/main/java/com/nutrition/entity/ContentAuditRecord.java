package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核记录实体类
 * 对应数据库表 content_audit_record
 */
@Data
@TableName("content_audit_record")
@Schema(description = "内容审核记录")
public class ContentAuditRecord extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键雪花ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "操作用户ID，关联sys_user")
    private Long userId;

    @TableField("openid")
    @Schema(description = "用户微信openid")
    private String openid;

    @TableField("audit_type")
    @Schema(description = "审核类型：1文本 2图片")
    private Integer auditType;

    @TableField("content_text")
    @Schema(description = "待审核文本内容，文本审核时存储")
    private String contentText;

    @TableField("file_ids")
    @Schema(description = "待审核图片附件ID，多张用英文逗号分隔")
    private String fileIds;

    @TableField("scene")
    @Schema(description = "业务场景值：1朋友圈动态 2评论 3饮食备注/描述 4个人资料")
    private Integer scene;

    @TableField("suggest")
    @Schema(description = "微信审核结果：pass放行/risky待复审/block违规拦截")
    private String suggest;

    @TableField("label")
    @Schema(description = "违规分类标签，如色情/广告/涉政")
    private String label;

    @TableField("audit_time")
    @Schema(description = "审核调用时间")
    private LocalDateTime auditTime;

    @TableField("review_status")
    @Schema(description = "人工复审状态：0无需复审 1待复审 2已处理")
    private Integer reviewStatus;

    @TableField("review_operator")
    @Schema(description = "复审管理员ID，后续开发")
    private Long reviewOperator;

    @TableField("review_result")
    @Schema(description = "人工复核结论：0合规 1确认违规")
    private Integer reviewResult;

    @TableField("review_remark")
    @Schema(description = "管理员复审备注")
    private String reviewRemark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField("delete_flag")
    @Schema(description = "逻辑删除 0正常 1已删除")
    private Integer deleteFlag;
}
