package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 饮食项实体类
 * 对应数据库表 diet_item
 */
@Data
@TableName("diet_item")
public class DietItem extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("record_id")
    @Schema(description = "关联diet_record主键")
    private Long recordId;

    @TableField("food_name")
    @Schema(description = "用户输入食物名称")
    private String foodName;

    @TableField("food_desc")
    @Schema(description = "食物描述，AI计算热量依据")
    private String foodDesc;

    @TableField("weight")
    @Schema(description = "食用重量(g)")
    private BigDecimal weight;

    @TableField("calories")
    @Schema(description = "总热量kcal（AI估算/手动填写）")
    private BigDecimal calories;

    @TableField("remark")
    @Schema(description = "单条食物备注")
    private String remark;

    @TableField("file_ids")
    @Schema(description = "食物图片")
    private String fileIds;
}