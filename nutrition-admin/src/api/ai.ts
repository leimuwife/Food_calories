import request, { ApiResponse } from '../utils/request'

export interface AiConfig {
  id: number
  modelName: string
  modelType: string
  apiUrl: string
  apiKey: string
  nickname: string
  systemPrompt: string
  temperature: number
  maxTokens: number
  isEnabled: number
}

export interface AiConfigForm {
  modelName: string
  modelType: string
  apiUrl: string
  apiKey: string
  nickname?: string
  systemPrompt?: string
  temperature: number
  maxTokens: number
}

export function getAiConfigList(): Promise<ApiResponse<AiConfig[]>> {
  return request.get('/ai/config/list')
}

export function addAiConfig(data: AiConfigForm): Promise<ApiResponse<number>> {
  return request.post('/ai/config', data)
}

export function updateAiConfig(id: number, data: AiConfigForm): Promise<ApiResponse<void>> {
  return request.put(`/ai/config/${id}`, data)
}

export function deleteAiConfig(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/ai/config/${id}`)
}

export function enableAiConfig(id: number): Promise<ApiResponse<void>> {
  return request.post(`/ai/config/${id}/enable`)
}

export function testAiConfig(message: string): Promise<ApiResponse<{ message: string; response: string }>> {
  return request.post('/ai/config/test', { message })
}