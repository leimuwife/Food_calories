package com.nutrition.service.impl;

import com.nutrition.client.FastApiClient;
import com.nutrition.service.AiModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI模型调用服务实现类（Mock版本）
 * 原真实LLM调用逻辑已迁移至独立Python-FastAPI项目
 * 当前返回预设兜底回答文案，后续接入FastAPI后替换为真实调用
 *
 * @see FastApiClient 后续通过此客户端调用Python-FastAPI AI服务
 */
@Service
@Slf4j
public class AiModelServiceImpl implements AiModelService {

    private static final String FALLBACK_RESPONSE = "感谢您的提问！我是食光笔记AI营养助手，当前AI服务正在升级中，"
            + "后续将为您提供更专业的营养分析与饮食建议。\n\n"
            + "⚠️ 答案由AI生成，仅供参考，不构成医疗建议";

    private static final String TEST_RESPONSE = "连通性测试成功！AI服务Mock模式运行正常。";

    @Override
    public String chat(String userMessage) {
        log.info("AI对话(Mock): message={}", userMessage);

        // TODO: 后续接入Python-FastAPI AI服务后，调用FastApiClient.chat()替换Mock实现
        return FALLBACK_RESPONSE;
    }

    @Override
    public String test(String testMessage) {
        log.info("AI配置测试(Mock): message={}", testMessage);

        // TODO: 后续接入Python-FastAPI AI服务后，调用FastApiClient.chat()替换Mock实现
        return TEST_RESPONSE;
    }
}
