package com.nutrition.service.impl;

import com.nutrition.client.FastApiClient;
import com.nutrition.dto.CalorieEstimateResultDTO;
import com.nutrition.entity.AiConfig;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.service.AiConfigService;
import com.nutrition.service.AiFoodEstimateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * AI食物热量估算服务实现类
 * 通过FastApiClient调用Python-FastAPI AI服务完成热量估算
 * Python端使用LCEL链式：Redis+RAG并行查询 → 外部Prompt → 大模型计算 → 结构化输出
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiFoodEstimateServiceImpl implements AiFoodEstimateService {

    private final FastApiClient fastApiClient;
    private final AiConfigService aiConfigService;

    @Override
    public CalorieEstimateResultDTO estimateCalorie(String foodName, String foodDesc, Integer weight) {
        log.info("AI热量估算: foodName={}, foodDesc={}, weight={}", foodName, foodDesc, weight);

        if (foodName == null || foodName.trim().isEmpty()) {
            log.warn("AI热量估算：食物名称为空");
            return CalorieEstimateResultDTO.builder().totalCalorie(BigDecimal.ZERO).build();
        }

        // 从MySQL获取当前启用的AI配置（含systemPrompt，传递给Python但热量估算不使用）
        AiConfig aiConfig = aiConfigService.getEnabledConfig();
        if (aiConfig == null) {
            log.warn("AI热量估算：未找到启用的AI配置");
            throw new com.nutrition.common.BusinessException(BizMsgEnum.AI_ESTIMATE_NO_CONFIG.getMessage());
        }

        // 调用Python-FastAPI热量估算接口
        BigDecimal totalCalorie = fastApiClient.estimateCalorie(
                foodName, foodDesc != null ? foodDesc : "", weight, aiConfig.getSystemPrompt());

        log.info("AI热量估算完成: foodName={}, calorie={}", foodName, totalCalorie);
        return CalorieEstimateResultDTO.builder().totalCalorie(totalCalorie).build();
    }
}
