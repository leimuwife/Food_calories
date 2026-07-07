package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 轻友圈评论实体类
 * 对应数据库表 feed_comment
 */
@Data
@TableName("feed_comment")
public class FeedComment extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("feed_id")
    @Schema(description = "动态ID")
    private Long feedId;

    @TableField("user_id")
    @Schema(description = "评论用户ID")
    private Long userId;

    @TableField("content")
    @Schema(description = "评论内容")
    private String content;
}