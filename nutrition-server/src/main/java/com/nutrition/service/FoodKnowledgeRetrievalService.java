package com.nutrition.service;

import com.nutrition.dto.KnowledgeDTO;

import java.util.List;

/**
 * 食物知识向量检索服务接口
 * 封装语义检索核心能力，供上层AI业务调用
 */
public interface FoodKnowledgeRetrievalService {

    /**
     * 语义检索食物知识（默认参数）
     * 默认召回Top3，最低相似度阈值0.75
     *
     * @param query 用户查询文本
     * @return 知识DTO列表，按相似度从高到低排序
     */
    List<KnowledgeDTO> retrieve(String query);

    /**
     * 语义检索食物知识（自定义参数）
     *
     * @param query 用户查询文本
     * @param topN 召回最大条数
     * @param minSimilarity 最低相似度阈值
     * @return 知识DTO列表，按相似度从高到低排序
     */
    List<KnowledgeDTO> retrieve(String query, int topN, double minSimilarity);
}