package com.nutrition.service.impl;

import com.nutrition.client.FastApiClient;
import com.nutrition.dto.AiChatResultDTO;
import com.nutrition.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI模型调用服务实现类
 * 真实对话与推理逻辑由独立Python-FastAPI服务的ReActAgent引擎提供，
 * 本类仅通过 {@link FastApiClient} 转发用户提问、会话与用户标识，并返回对话结果。
 *
 * <p>会话标识流转：首次对话 sessionId 传 null，Python 回调 Java 雪花算法
 * 生成正式会话ID并随结果回传；Controller 将其返回前端，后续多轮对话原样携带。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final FastApiClient fastApiClient;

    @Override
    public AiChatResultDTO chat(String userMessage, String sessionId, Long userId) {
        log.info("AI对话请求转发Python: sessionId={}, userId={}, message={}",
                sessionId, userId, userMessage);
        return fastApiClient.chat(userMessage, sessionId, userId);
    }

    @Override
    public AiChatResultDTO test(String testMessage, Long userId) {
        log.info("AI配置连通性测试转发Python: userId={}, message={}", userId, testMessage);
        // 复用完整对话链路：新建会话走通即证明Python服务、会话回调、模型配置全部可用
        return fastApiClient.chat(testMessage, null, userId);
    }
}
