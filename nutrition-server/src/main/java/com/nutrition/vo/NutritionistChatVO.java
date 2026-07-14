package com.nutrition.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI营养师对话响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NutritionistChatVO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 角色类型 user/assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 图片URL列表
     */
    private List<String> images;

    /**
     * 文件ID列表
     */
    private List<String> fileIds;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 构建AI响应VO
     */
    public static NutritionistChatVO buildAssistant(String content) {
        return NutritionistChatVO.builder()
                .id(System.currentTimeMillis())
                .role("assistant")
                .content(content)
                .images(List.of())
                .fileIds(List.of())
                .createTime(LocalDateTime.now().toString())
                .build();
    }
}