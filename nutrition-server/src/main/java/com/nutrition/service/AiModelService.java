package com.nutrition.service;

import com.nutrition.dto.AiChatResultDTO;

/**
 * AI模型调用服务接口
 * 真实对话能力由Python-FastAPI的ReActAgent引擎提供
 */
public interface AiModelService {

    /**
     * 调用大模型进行对话
     *
     * @param userMessage 用户提问内容
     * @param sessionId   会话ID；为null表示新建对话，由Python回调Java雪花算法生成并在结果中回传
     * @param userId      当前登录用户ID；新建会话时必传
     * @return 对话结果（AI回答 + 会话ID + 是否新建会话）
     */
    AiChatResultDTO chat(String userMessage, String sessionId, Long userId);

    /**
     * 测试AI配置连通性（复用对话链路，能正常返回回答即说明Python服务与模型配置可用）
     *
     * @param testMessage 测试问题
     * @param userId      当前登录用户ID（测试同样会创建正式会话以完整验证链路）
     * @return 对话结果
     */
    AiChatResultDTO test(String testMessage, Long userId);
}
