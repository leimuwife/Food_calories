package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态点赞记录实体类
 * 对应数据库表 feed_like
 */
@Data
@TableName("feed_like")
public class FeedLike extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("feed_id")
    @Schema(description = "动态ID")
    private Long feedId;

    @TableField("user_id")
    @Schema(description = "点赞用户ID")
    private Long userId;
}