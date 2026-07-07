package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户打卡记录实体类
 * 对应数据库表 checkin_record
 */
@Data
@TableName("checkin_record")
public class CheckinRecord extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户ID")
    private Long userId;

    @TableField("checkin_date")
    @Schema(description = "打卡日期")
    private LocalDate checkinDate;
}