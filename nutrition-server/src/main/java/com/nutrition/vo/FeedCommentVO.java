package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态评论视图对象
 */
@Data
@Schema(description = "动态评论视图对象")
public class FeedCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "评论用户ID")
    private Long userId;

    @Schema(description = "评论用户昵称")
    private String userName;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "创建时间")
    private String createTime;
}