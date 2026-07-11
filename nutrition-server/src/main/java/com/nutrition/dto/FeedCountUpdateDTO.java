package com.nutrition.dto;

import lombok.Data;

/**
 * 动态计数批量更新 DTO
 * 用于定时任务批量同步 Redis 计数到 MySQL
 */
@Data
public class FeedCountUpdateDTO {

    /**
     * 动态ID
     */
    private Long feedId;

    /**
     * 点赞数（可为null，表示不更新此字段）
     */
    private Integer likeCount;

    /**
     * 评论数（可为null，表示不更新此字段）
     */
    private Integer commentCount;
}
