package com.nutrition.service;

/**
 * 健康分析 AI 服务接口。
 * 封装专用报告调用，便于业务层重试和单元测试。
 */
public interface HealthAnalysisAiService {

    /**
     * 调用 AI 生成健康分析报告。
     *
     * @param prompt 完整分析提示词
     * @param userId 当前用户ID
     * @return AI 报告文本
     */
    String generateReport(String prompt, Long userId);
}
