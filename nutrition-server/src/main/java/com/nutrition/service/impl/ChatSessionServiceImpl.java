package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nutrition.common.BusinessException;
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
import java.time.LocalDateTime;

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
        return queryHistory(sessionId, limit, null);
    }

    /**
     * 获取指定用户可展示的会话历史。
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param limit     最多返回条数
     * @return 用户可见的正序消息列表
     */
    @Override
    public List<ChatMessageVO> getVisibleHistory(Long sessionId, Long userId, int limit) {
        if (userId == null) {
            throw new BusinessException(BizMsgEnum.USER_NOT_LOGIN);
        }
        NutritionistChat chat = chatSessionMapper.selectById(sessionId);
        if (chat == null) {
            throw new BusinessException(BizMsgEnum.CHAT_SESSION_NOT_FOUND);
        }
        if (!userId.equals(chat.getUserId())) {
            throw new BusinessException(BizMsgEnum.CHAT_NO_PERMISSION_VIEW);
        }
        return queryHistory(sessionId, limit,
                List.of(ChatRoleEnum.USER.getCode(), ChatRoleEnum.AI_ANSWER.getCode()));
    }

    @Override
    public List<ChatSessionVO> listSessionsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(BizMsgEnum.CHAT_USER_ID_EMPTY.getMessage());
        }

        // 查询该用户未被逻辑删除的会话，按最后更新时间倒序
        List<NutritionistChat> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<NutritionistChat>()
                        .eq(NutritionistChat::getUserId, userId)
                        .orderByDesc(NutritionistChat::getUpdateTime)
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
     * 按条件查询聊天历史并转换为视图对象。
     *
     * @param sessionId 会话ID
     * @param limit     最多返回条数
     * @param roleCodes 角色编码过滤条件；为空时查询全部角色
     * @return 正序排列的历史消息
     */
    private List<ChatMessageVO> queryHistory(Long sessionId, int limit, List<Long> roleCodes) {
        if (sessionId == null) {
            throw new IllegalArgumentException(BizMsgEnum.CHAT_SESSION_ID_EMPTY.getMessage());
        }
        int queryLimit = limit > 0 ? limit : 20;

        LambdaQueryWrapper<AIChatMessage> query = new LambdaQueryWrapper<AIChatMessage>()
                .eq(AIChatMessage::getSessionId, sessionId)
                .eq(AIChatMessage::getDeleteFlag, 0);
        if (roleCodes != null && !roleCodes.isEmpty()) {
            query.in(AIChatMessage::getRole, roleCodes);
        }
        query.orderByDesc(AIChatMessage::getId).last("LIMIT " + queryLimit);

        List<AIChatMessage> messages = chatMessageMapper.selectList(query);
        Collections.reverse(messages);

        List<ChatMessageVO> voList = new ArrayList<>(messages.size());
        for (AIChatMessage msg : messages) {
            voList.add(ChatMessageVO.builder()
                    .role(ChatRoleEnum.getRoleByCode(msg.getRole()))
                    .content(msg.getContent())
                    .createTime(msg.getCreateTime())
                    .build());
        }
        log.info("查询AI聊天历史: sessionId={}, limit={}, count={}", sessionId, queryLimit, voList.size());
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

        // 3. 更新会话时间，让最近使用的会话排在列表首位
        chat.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(chat);
        log.info("会话消息批量落盘完成: sessionId={}, count={}", sessionId, messages.size());
    }
}
