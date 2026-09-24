package com.nutrition.service.impl;

import com.nutrition.client.FastApiClient;
import com.nutrition.service.HealthAnalysisAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 健康分析 AI 服务实现。
 */
@Service
@RequiredArgsConstructor
public class HealthAnalysisAiServiceImpl implements HealthAnalysisAiService {

    /** Python AI 客户端 */
    private final FastApiClient fastApiClient;

    /**
     * 调用 Python 专用健康报告接口。
     *
     * @param prompt 完整分析提示词
     * @param userId 当前用户ID
     * @return AI 报告文本
     */
    @Override
    public String generateReport(String prompt, Long userId) {
        return fastApiClient.generateHealthReport(prompt, userId);
    }
}
