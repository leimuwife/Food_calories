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
export interface AdminUser {
  id: number | string
  username: string
  nickname: string
  password: string
  createTime: string
  updateTime: string
  deleteFlag: number | string
  enabled: boolean
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 分页查询全部注册用户 */
export function getAdminUserPage(pageNum = 1, pageSize = 10): Promise<ApiResponse<PageResult<AdminUser>>> {
  return request.get('/admin/users', { params: { pageNum, pageSize } })
}

/** 启用或禁用用户 */
export function updateAdminUserStatus(userId: number | string, enabled: boolean): Promise<ApiResponse<AdminUser>> {
  return request.put(`/admin/users/${userId}/status`, { enabled })
}
