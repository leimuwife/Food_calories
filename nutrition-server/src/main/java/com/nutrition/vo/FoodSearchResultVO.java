package com.nutrition.vo;

import lombok.Data;
import java.util.List;

/**
 * 食物搜索结果视图对象
 * 用于返回食物搜索结果列表及总数
 */
@Data
public class FoodSearchResultVO {

    private List<FoodVO> list;

    private Integer total;
}
