package com.nutrition.service;

import com.nutrition.dto.CalorieEstimateResultDTO;

/**
 * AI食物热量估算服务接口
 * 提供食物热量智能估算功能，与AI营养师对话接口完全隔离
 */
public interface AiFoodEstimateService {

    /**
     * AI估算食物热量
     * 用户输入食物名称、描述和重量，一键估算总热量，仅返回数值用于前端输入框回填
     *
     * @param foodName 食物名称（如"牛蛙"）
     * @param foodDesc 食物描述（如"一只"，可为空）
     * @param weight   用户输入的食物重量（单位：克），可为null，若为null则默认200g
     * @return CalorieEstimateResultDTO 热量估算结果，仅包含总热量数值
     */
    CalorieEstimateResultDTO estimateCalorie(String foodName, String foodDesc, Integer weight);
}