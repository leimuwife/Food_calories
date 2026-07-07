import request from '../request'
import type { NutritionistChatParam, NutritionistChatResult } from '../types'

export function nutritionistChat(data: NutritionistChatParam) {
  return request<NutritionistChatResult>({
    url: '/api/nutritionist/chat',
    method: 'POST',
    data,
  })
}