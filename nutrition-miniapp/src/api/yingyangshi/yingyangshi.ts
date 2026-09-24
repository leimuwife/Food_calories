import request from '../request'
import type {
  ChatHistoryMessage,
  ChatSessionVO,
  NutritionistChatParam,
  NutritionistChatResult,
} from '../types'

export function nutritionistChat(data: NutritionistChatParam) {
  return request<NutritionistChatResult>({
    url: '/api/ai/chat',
    method: 'POST',
    data: {
      content: data.content,
      // 有会话ID时携带以保持多轮上下文；为空则新建对话
      ...(data.sessionId ? { sessionId: data.sessionId } : {}),
    },
    // 页面已有“AI正在输入”动画，关闭全局loading遮罩；ReAct多轮+工具调用耗时较长，超时放宽到120s
    showLoading: false,
    timeout: 120000,
  })
}

/** 查询当前用户的会话列表 */
export function getChatSessions() {
  return request<ChatSessionVO[]>({
    url: '/api/chat/session/list',
    method: 'GET',
    showLoading: false,
  })
}

/** 查询当前用户指定会话的可见历史消息 */
export function getChatHistory(sessionId: string) {
  return request<ChatHistoryMessage[]>({
    url: `/api/chat/session/${sessionId}/messages`,
    method: 'GET',
    params: { limit: 100 },
    showLoading: false,
  })
}
