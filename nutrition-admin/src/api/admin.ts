import request, { ApiResponse } from '../utils/request'

export interface AdminLoginResult {
  token: string
  id: number
  username: string
  nickname: string
  fileIds: string
  phone: string
}

export function adminLogin(data: { username: string; password: string }): Promise<ApiResponse<AdminLoginResult>> {
  return request.post('/admin/login', data)
}