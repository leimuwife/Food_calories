package com.nutrition.service;

import com.nutrition.dto.NutritionDTO;

import java.util.List;
import java.util.Map;

/**
 * 食物营养数据服务接口
 * 提供食物营养数据的查询、缓存管理等功能
 * 支持Redis缓存优先查询，MySQL兜底
 */
public interface FoodNutritionService {

    /**
     * 批量查询食材营养数据（供AI热量估算调用）
     * 优先从Redis Hash中批量查询，未命中的去MySQL查询并同步回写缓存
     *
     * @param foodNameList 食物名称列表
     * @return Map<String, NutritionDTO> key为食物名称，value为精简营养数据DTO
     */
    Map<String, NutritionDTO> batchGetNutrition(List<String> foodNameList);

    /**
     * 模糊匹配兜底查询
     * 当精确查询无结果时，按食物名称前缀模糊匹配MySQL，返回最匹配的1条结果
     *
     * @param keyword 食物名称关键词
     * @return NutritionDTO 精简营养数据DTO，未匹配到返回null
     */
    NutritionDTO fuzzyMatchNutrition(String keyword);

    /**
     * 将食物营养数据同步写入Redis缓存
     *
     * @param foodName 食物名称
     * @param nutritionDTO 营养数据DTO
     */
    void syncToCache(String foodName, NutritionDTO nutritionDTO);
}