package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 动态列表项视图对象
 */
@Data
@Schema(description = "动态列表项视图对象")
public class FeedItemVO {

    @Schema(description = "动态ID")
    private Long id;

    @Schema(description = "发布用户ID")
    private Long userId;

    @Schema(description = "发布用户昵称")
    private String userName;

    @Schema(description = "发布用户头像")
    private String userAvatar;

    @Schema(description = "动态内容")
    private String content;

    @Schema(description = "附件ID数组")
    private List<String> fileIds;

    @Schema(description = "图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "评论列表（返回前3条）")
    private List<FeedCommentVO> comments;

    @Schema(description = "发布时间（格式化）")
    private String publishTime;

    @Schema(description = "创建时间")
    private String createTime;
}