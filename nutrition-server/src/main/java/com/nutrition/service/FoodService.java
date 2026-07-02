package com.nutrition.service;

import com.nutrition.entity.FoodDict;
import com.nutrition.vo.FoodCategoryVO;
import com.nutrition.vo.FoodSearchResultVO;
import com.nutrition.vo.FoodVO;

import java.util.List;

public interface FoodService {

    FoodSearchResultVO search(String keyword, String category);

    FoodVO getDetail(Long foodId);

    List<FoodCategoryVO> getCategories();

    int batchImport(List<FoodDict> foodList);

    FoodVO convertToVO(FoodDict food);
}
