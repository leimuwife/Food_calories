package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.FoodNutrition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食物营养数据访问层
 * 负责 food_nutrition 表的数据库操作
 */
@Mapper
public interface FoodNutritionMapper extends BaseMapper<FoodNutrition> {
}