package com.nutrition.service;

import com.nutrition.dto.ChatMessageItemDTO;
import com.nutrition.vo.ChatMessageVO;
import com.nutrition.vo.ChatSessionVO;

import java.util.List;

/**
 * AI聊天会话服务
 * 供Python session_service 回调完成 chat_session / chat_message 的MySQL持久化
 */
public interface ChatSessionService {

    /**
     * 创建全新聊天会话
     *
     * @param userId 用户ID（必传）
     * @return 雪花生成的会话ID
     */
    Long createSession(Long userId);

    /**
     * 获取会话最近的历史消息（未被逻辑删除，按时间倒序取最近N条后转正序返回）
     *
     * @param sessionId 会话ID
     * @param limit 最多返回条数
     * @return 历史消息列表
     */
    List<ChatMessageVO> getRecentHistory(Long sessionId, int limit);

    /**
     * 查询用户的历史会话列表（未逻辑删除，按创建时间倒序，附最近一条用户消息预览）
     * 供前端进入聊天页加载会话列表
     *
     * @param userId 用户ID
     * @return 会话列表项（按时间倒序）
     */
    List<ChatSessionVO> listSessionsByUserId(Long userId);

    /**
     * 批量落盘消息（Python flush_session_to_mysql 调用）
     * 会话不存在时自动创建，消息一次性批量插入
     *
     * @param sessionId 会话ID
     * @param userId 用户ID（会话不存在创建时使用，可空）
     * @param messages 消息列表
     */
    void flushMessages(Long sessionId, Long userId, List<ChatMessageItemDTO> messages);
}
