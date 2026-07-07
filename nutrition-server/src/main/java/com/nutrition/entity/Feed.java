package com.nutrition.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 轻友圈动态实体类
 * 对应数据库表 feed
 */
@Data
@TableName("feed")
public class Feed extends Common {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "雪花主键ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "发布用户ID")
    private Long userId;

    @TableField("content")
    @Schema(description = "动态文字内容")
    private String content;

    @TableField("file_ids")
    @Schema(description = "图片ID JSON数组")
    private String fileIds;

    @TableField("like_count")
    @Schema(description = "点赞总数")
    private Integer likeCount;

    @TableField("comment_count")
    @Schema(description = "评论总数")
    private Integer commentCount;
}