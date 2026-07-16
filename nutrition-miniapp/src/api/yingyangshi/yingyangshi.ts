import request from '../request'
import type { NutritionistChatParam, NutritionistChatResult } from '../types'

export function nutritionistChat(data: NutritionistChatParam) {
  return request<{ response: string }>({
    url: '/api/ai/chat',
    method: 'POST',
    data: { content: data.content },
  })
}