package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nutrition.enums.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 饮食记录实体类
 * 对应数据库表 diet_record
 */
@Data
@TableName("diet_record")
public class DietRecord extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("record_date")
    @Schema(description = "饮食记录日期")
    private LocalDate recordDate;

    @TableField("meal_type")
    @Schema(description = "餐次：breakfast/lunch/dinner/snack（代码枚举控制）")
    private MealType mealType;
}