import request from '../request'
import type { CaptchaVO, LoginResultVO, RegisterParam, ResetPasswordParam } from '../types'

/** 获取注册用图形验证码 */
export function getCaptcha() {
  return request<CaptchaVO>({
    url: '/api/auth/captcha',
    method: 'GET',
    showLoading: false,
  })
}

/** 使用用户名和密码登录 */
export function login(username: string, password: string) {
  return request<LoginResultVO>({
    url: '/api/auth/login',
    method: 'POST',
    data: { username, password },
    showLoading: false,
  })
}

/** 注册普通用户账号 */
export function register(data: RegisterParam) {
  return request<void>({
    url: '/api/auth/register',
    method: 'POST',
    data,
    showLoading: false,
  })
}

/** 重置密码：通过用户名 + 注册手机号校验身份 */
export function resetPassword(data: ResetPasswordParam) {
  return request<void>({
    url: '/api/auth/reset-password',
    method: 'POST',
    data,
    showLoading: false,
  })
}
