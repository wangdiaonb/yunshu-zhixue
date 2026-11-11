import { http } from './request'
import type { AdminLoginPayload, AdminLoginResult } from '@/types/auth'
import { getRefreshToken } from '@/utils/token'

/** A-A01 后台登录（账号为用户名或手机号） */
export function login(payload: AdminLoginPayload): Promise<AdminLoginResult> {
  return http.post('/auth/login', payload, { skipAuthRefresh: true })
}

/** A-A03 登出（幂等） */
export function logout(): Promise<void> {
  return http.post('/auth/logout', null, {
    silent: true,
    skipAuthRefresh: true,
    headers: { 'X-Refresh-Token': getRefreshToken() }
  })
}
