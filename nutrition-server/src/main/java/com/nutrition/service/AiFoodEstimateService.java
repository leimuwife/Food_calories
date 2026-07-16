package com.nutrition.service;

import com.nutrition.dto.CalorieEstimateResultDTO;

/**
 * AI食物热量估算服务接口
 * 提供食物热量智能估算功能，与AI营养师对话接口完全隔离
 */
public interface AiFoodEstimateService {

    /**
     * AI估算食物热量
     * 用户输入食物描述和重量，一键估算总热量，仅返回数值用于前端输入框回填
     *
     * @param foodDesc 用户输入的食物描述（如"一块含西瓜、面包、动物奶油的蛋糕"）
     * @param weight   用户输入的食物重量（单位：克），可为null，若为null则从描述中提取
     * @return CalorieEstimateResultDTO 热量估算结果，仅包含总热量数值
     */
    CalorieEstimateResultDTO estimateCalorie(String foodDesc, Integer weight);
}