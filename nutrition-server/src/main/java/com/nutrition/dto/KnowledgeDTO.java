package com.nutrition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 食物知识检索结果DTO
 * 用于封装向量检索返回的食物知识数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "食物知识检索结果")
public class KnowledgeDTO {

    @Schema(description = "食物ID")
    private Long foodId;

    @Schema(description = "食物名称")
    private String foodName;

    @Schema(description = "知识文本内容")
    private String content;

    @Schema(description = "相似度分数（0-1）")
    private Double similarity;
}