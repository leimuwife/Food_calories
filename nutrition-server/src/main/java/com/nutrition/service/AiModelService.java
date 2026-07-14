package com.nutrition.service;

/**
 * AI模型调用服务接口
 */
public interface AiModelService {

    /**
     * 调用大模型进行对话
     *
     * @param userMessage 用户提问内容
     * @return AI回答内容
     */
    String chat(String userMessage);

    /**
     * 测试配置连通性
     *
     * @param testMessage 测试问题
     * @return AI回答内容
     */
    String test(String testMessage);
}