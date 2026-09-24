package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 食物分类视图对象。
 */
@Data
@Builder
@Schema(description = "食物分类")
public class FoodCategoryVO {

    /** 分类名称 */
    private String category;
}
