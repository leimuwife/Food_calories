package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 食物搜索分页结果视图对象。
 */
@Data
@Builder
@Schema(description = "食物搜索分页结果")
public class FoodSearchResultVO {

    /** 当前页食物列表 */
    private List<FoodVO> list;

    /** 符合条件的总条数 */
    private Long total;
}
