package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nutrition.dto.ChatMessageItemDTO;
import com.nutrition.entity.AIChatMessage;
import com.nutrition.entity.NutritionistChat;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.ChatRoleEnum;
import com.nutrition.mapper.ChatMessageMapper;
import com.nutrition.mapper.ChatSessionMapper;
import com.nutrition.service.ChatSessionService;
import com.nutrition.vo.ChatMessageVO;
import com.nutrition.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI聊天会话服务实现
 * 职责：chat_session / chat_message 的MySQL持久化，供Python session_service回调
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Long createSession(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(BizMsgEnum.CHAT_USER_ID_EMPTY.getMessage());
        }

        NutritionistChat chat = new NutritionistChat();
        chat.setUserId(userId);
        chatSessionMapper.insert(chat);

        log.info("创建AI聊天会话成功: sessionId={}, userId={}", chat.getSessionId(), userId);
        return chat.getSessionId();
    }

    @Override
    public List<ChatMessageVO> getRecentHistory(Long sessionId, int limit) {
        if (sessionId == null) {
            throw new IllegalArgumentException(BizMsgEnum.CHAT_SESSION_ID_EMPTY.getMessage());
        }
        if (limit <= 0) {
            limit = 20;
        }

        // 查询最近limit条未逻辑删除的消息（按ID倒序取最近，再转正序返回）
        List<AIChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<AIChatMessage>()
                        .eq(AIChatMessage::getSessionId, sessionId)
                        .eq(AIChatMessage::getDeleteFlag, 0)
                        .orderByDesc(AIChatMessage::getId)
                        .last("LIMIT " + limit)
        );
        Collections.reverse(messages);

        List<ChatMessageVO> voList = new ArrayList<>(messages.size());
        for (AIChatMessage msg : messages) {
            voList.add(ChatMessageVO.builder()
                    .role(ChatRoleEnum.getRoleByCode(msg.getRole()))
                    .content(msg.getContent())
                    .createTime(msg.getCreateTime())
                    .build());
        }
        log.info("查询AI聊天历史: sessionId={}, limit={}, count={}", sessionId, limit, voList.size());
        return voList;
    }

    @Override
    public List<ChatSessionVO> listSessionsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(BizMsgEnum.CHAT_USER_ID_EMPTY.getMessage());
        }

        // 查询该用户未被逻辑删除的会话（全局逻辑删除自动过滤delete_flag=0），按创建时间倒序
        List<NutritionistChat> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<NutritionistChat>()
                        .eq(NutritionistChat::getUserId, userId)
                        .orderByDesc(NutritionistChat::getSessionId)
        );

        List<ChatSessionVO> voList = new ArrayList<>(sessions.size());
        for (NutritionistChat chat : sessions) {
            // 取该会话最近一条用户消息作为列表标题预览
            String lastMessage = queryLastUserMessage(chat.getSessionId());
            voList.add(ChatSessionVO.builder()
                    .sessionId(chat.getSessionId())
                    .userId(chat.getUserId())
                    .lastMessage(lastMessage)
                    .createTime(chat.getCreateTime())
                    .updateTime(chat.getUpdateTime())
                    .build());
        }
        log.info("查询AI聊天会话列表: userId={}, count={}", userId, voList.size());
        return voList;
    }

    /**
     * 查询会话最近一条用户消息（作为会话列表标题预览）
     *
     * @param sessionId 会话ID
     * @return 最近一条用户消息内容；无则返回null
     */
    private String queryLastUserMessage(Long sessionId) {
        List<AIChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<AIChatMessage>()
                        .eq(AIChatMessage::getSessionId, sessionId)
                        .eq(AIChatMessage::getRole, ChatRoleEnum.USER.getCode())
                        .eq(AIChatMessage::getDeleteFlag, 0)
                        .orderByDesc(AIChatMessage::getId)
                        .last("LIMIT 1")
        );
        if (messages.isEmpty()) {
            return null;
        }
        String content = messages.get(0).getContent();
        // 预览截断，避免列表过长
        return content != null && content.length() > 30 ? content.substring(0, 30) + "…" : content;
    }

    @Override
    @Transactional
    public void flushMessages(Long sessionId, Long userId, List<ChatMessageItemDTO> messages) {
        if (sessionId == null) {
            throw new IllegalArgumentException(BizMsgEnum.CHAT_SESSION_ID_EMPTY.getMessage());
        }
        if (messages == null || messages.isEmpty()) {
            log.info("会话无消息可落盘，跳过: sessionId={}", sessionId);
            return;
        }

        // 1. 会话不存在则自动创建（Python侧可能因缓存先于会话异常场景）
        NutritionistChat chat = chatSessionMapper.selectById(sessionId);
        if (chat == null) {
            chat = new NutritionistChat();
            chat.setSessionId(sessionId);
            chat.setUserId(userId);
            chatSessionMapper.insert(chat);
            log.info("会话不存在，落盘时自动创建: sessionId={}, userId={}", sessionId, userId);
        }

        // 2. 批量插入消息（角色字符串→编码映射）
        for (ChatMessageItemDTO item : messages) {
            if (item.getContent() == null || item.getContent().trim().isEmpty()) {
                log.warn("消息内容为空，跳过: sessionId={}", sessionId);
                continue;
            }
            Long roleCode = ChatRoleEnum.getCodeByRole(item.getRole());
            if (roleCode == null) {
                log.warn("非法消息角色，跳过: sessionId={}, role={}", sessionId, item.getRole());
                continue;
            }

            AIChatMessage msg = new AIChatMessage();
            msg.setSessionId(sessionId);
            msg.setRole(roleCode);
            msg.setContent(item.getContent());
            chatMessageMapper.insert(msg);
        }
        log.info("会话消息批量落盘完成: sessionId={}, count={}", sessionId, messages.size());
    }
}
