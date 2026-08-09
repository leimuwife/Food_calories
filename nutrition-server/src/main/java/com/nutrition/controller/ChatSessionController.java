package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.dto.ChatMessageFlushDTO;
import com.nutrition.dto.ChatSessionCreateDTO;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.service.ChatSessionService;
import com.nutrition.vo.ChatMessageVO;
import com.nutrition.vo.ChatSessionCreateVO;
import com.nutrition.vo.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI聊天会话管理控制器
 * 供Python session_service 回调完成 chat_session / chat_message 的MySQL持久化
 * 同时为前端提供会话列表接口（进入聊天页加载历史会话）
 * 路径前缀 /api 由 WebConfig 统一添加
 */
@RestController
@RequestMapping("/chat/session")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI聊天会话管理", description = "Python会话服务回调：创建会话、查询历史、批量落盘；前端会话列表")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 创建全新聊天会话（Python create_session 调用）
     * 会话ID由Java雪花算法生成并返回
     *
     * @param dto 请求体（含userId）
     * @return 会话ID
     */
    @PostMapping("/create")
    @Operation(summary = "创建AI聊天会话", description = "插入chat_session记录，返回雪花生成的sessionId")
    public Result<ChatSessionCreateVO> createSession(@RequestBody ChatSessionCreateDTO dto) {
        log.info("创建AI聊天会话请求: userId={}", dto.getUserId());

        if (dto.getUserId() == null) {
            return Result.badRequest(BizMsgEnum.CHAT_USER_ID_EMPTY.getMessage());
        }

        try {
            Long sessionId = chatSessionService.createSession(dto.getUserId());
            ChatSessionCreateVO vo = ChatSessionCreateVO.builder()
                    .sessionId(sessionId)
                    .userId(dto.getUserId())
                    .build();
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("{}: userId={}, error={}",
                    BizMsgEnum.CHAT_CREATE_FAILED.getMessage(), dto.getUserId(), e.getMessage(), e);
            return Result.fail(BizMsgEnum.CHAT_CREATE_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * 查询会话最近历史消息（Python get_recent_history 缓存未命中时调用）
     *
     * @param sessionId 会话ID
     * @param limit 最多返回条数（默认20）
     * @return 历史消息列表（正序）
     */
    @GetMapping("/{sessionId}/history")
    @Operation(summary = "查询会话历史消息", description = "查询未被逻辑删除的最近N条消息，正序返回")
    public Result<List<ChatMessageVO>> getRecentHistory(
            @PathVariable("sessionId") Long sessionId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        log.info("查询AI聊天历史请求: sessionId={}, limit={}", sessionId, limit);

        try {
            List<ChatMessageVO> history = chatSessionService.getRecentHistory(sessionId, limit);
            return Result.ok(history);
        } catch (Exception e) {
            log.error("{}: sessionId={}, error={}",
                    BizMsgEnum.CHAT_HISTORY_LOAD_FAILED.getMessage(), sessionId, e.getMessage(), e);
            return Result.fail(BizMsgEnum.CHAT_HISTORY_LOAD_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * 查询用户的历史会话列表（前端进入聊天页加载，类比豆包会话列表）
     *
     * @param userId 用户ID
     * @return 会话列表项（按时间倒序，附最近一条用户消息预览）
     */
    @GetMapping("/list")
    @Operation(summary = "查询用户会话列表", description = "按userId查询未逻辑删除的会话列表，按创建时间倒序，附最近消息预览")
    public Result<List<ChatSessionVO>> listSessions(@RequestParam("userId") Long userId) {
        log.info("查询AI聊天会话列表请求: userId={}", userId);

        if (userId == null) {
            return Result.badRequest(BizMsgEnum.CHAT_USER_ID_EMPTY.getMessage());
        }

        try {
            List<ChatSessionVO> sessions = chatSessionService.listSessionsByUserId(userId);
            return Result.ok(sessions);
        } catch (Exception e) {
            log.error("{}: userId={}, error={}",
                    BizMsgEnum.CHAT_SESSION_LIST_FAILED.getMessage(), userId, e.getMessage(), e);
            return Result.fail(BizMsgEnum.CHAT_SESSION_LIST_FAILED.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * 批量落盘会话消息（Python flush_session_to_mysql 调用）
     * 会话不存在时自动创建，消息一次性批量插入
     *
     * @param dto 请求体（sessionId + messages）
     * @return 操作结果
     */
    @PostMapping("/flush")
    @Operation(summary = "批量落盘会话消息", description = "会话不存在自动创建，消息批量插入chat_message")
    public Result<Void> flushMessages(@RequestBody ChatMessageFlushDTO dto) {
        log.info("会话消息批量落盘请求: sessionId={}, messageCount={}",
                dto.getSessionId(),
                dto.getMessages() == null ? 0 : dto.getMessages().size());

        if (dto.getSessionId() == null) {
            return Result.badRequest(BizMsgEnum.CHAT_SESSION_ID_EMPTY.getMessage());
        }

        try {
            chatSessionService.flushMessages(dto.getSessionId(), dto.getUserId(), dto.getMessages());
            return Result.ok("消息落盘成功", null);
        } catch (Exception e) {
            log.error("{}: sessionId={}, error={}",
                    BizMsgEnum.CHAT_FLUSH_FAILED.getMessage(), dto.getSessionId(), e.getMessage(), e);
            return Result.fail(BizMsgEnum.CHAT_FLUSH_FAILED.getMessage() + ": " + e.getMessage());
        }
    }
}
