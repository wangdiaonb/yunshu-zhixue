import { http } from './request'
import type { PortalProfile } from '@/types/auth'

/** P-M01 个人信息 */
export function getProfile(): Promise<PortalProfile> {
  return http.get('/profile')
}

/** P-M02 修改资料 */
export function updateProfile(payload: Partial<Pick<PortalProfile, 'nickname' | 'email' | 'gender' | 'preferredSubjectId' | 'avatar'>>): Promise<void> {
  return http.put('/profile', payload)
}

/** P-M03 修改密码（成功后所有令牌失效，需重新登录） */
export function changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
  return http.put('/profile/password', payload)
}
