package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录表实体
 */
@Data
@TableName("diet_record")
public class DietRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 记录日期 */
    private LocalDate recordDate;

    /** 餐次类型: breakfast/lunch/dinner/snack */
    private String mealType;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---- 非数据库字段 ----
    /** 明细列表（仅用于查询组装，不映射数据库） */
    @TableField(exist = false)
    private java.util.List<DietRecordItem> items;
}
