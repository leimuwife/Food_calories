package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 饮食记录明细表实体
 */
@Data
@TableName("diet_record_item")
public class DietRecordItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属记录 ID */
    private Long recordId;

    /** 食物 ID */
    private Long foodId;

    /** 食物名称快照 */
    private String foodName;

    /** 食用重量(g) */
    private Integer weight;

    /** 总热量(kcal) */
    private Integer calories;

    /** 总蛋白质(g) */
    private Double protein;

    /** 总脂肪(g) */
    private Double fat;

    /** 总碳水(g) */
    private Double carbs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
