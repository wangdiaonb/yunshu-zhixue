import { http } from './request'
import type {
  LoginResult,
  PasswordLoginPayload,
  RegisterPayload,
  ResetPasswordPayload,
  ResetVerifyPayload,
  ResetVerifyResult,
  SmsCodePayload,
  SmsLoginPayload
} from '@/types/auth'
import { getRefreshToken } from '@/utils/token'

/** P-A01 发送短信验证码（debugCode 仅开发环境返回） */
export function sendSmsCode(
  payload: SmsCodePayload
): Promise<{ expireSeconds: number; debugCode?: string | null }> {
  return http.post('/auth/sms-code', payload)
}

/** P-A02 注册（成功后直接签发双令牌） */
export function register(payload: RegisterPayload): Promise<LoginResult> {
  return http.post('/auth/register', payload, { skipAuthRefresh: true })
}

/** P-A03 密码登录 */
export function loginByPassword(payload: PasswordLoginPayload): Promise<LoginResult> {
  return http.post('/auth/login/password', payload, { skipAuthRefresh: true })
}

/** P-A04 验证码登录 */
export function loginBySms(payload: SmsLoginPayload): Promise<LoginResult> {
  return http.post('/auth/login/sms', payload, { skipAuthRefresh: true })
}

/** P-A05 找回密码·验证验证码 */
export function verifyResetCode(payload: ResetVerifyPayload): Promise<ResetVerifyResult> {
  return http.post('/auth/reset/verify', payload, { skipAuthRefresh: true })
}

/** P-A06 找回密码·重置 */
export function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  return http.post('/auth/reset', payload, { skipAuthRefresh: true })
}

/** P-A08 登出（幂等） */
export function logout(): Promise<void> {
  return http.post('/auth/logout', null, {
    silent: true,
    skipAuthRefresh: true,
    headers: { 'X-Refresh-Token': getRefreshToken() }
  })
}
