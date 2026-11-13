import { http } from './request'
import type { PortalProfile } from '@/types/auth'
import { getRefreshToken } from '@/utils/token'

/** P-M01 个人信息 */
export function getProfile(): Promise<PortalProfile> {
  return http.get('/profile')
}

/** P-M02 修改资料（返回更新后的个人资料） */
export function updateProfile(
  payload: Partial<Pick<PortalProfile, 'nickname' | 'email' | 'gender' | 'preferredSubjectId' | 'avatar'>>
): Promise<PortalProfile> {
  return http.put('/profile', payload)
}

/** P-M03 修改密码（成功后当前登录态失效，需重新登录） */
export function changePassword(payload: { oldPassword: string; newPassword: string }): Promise<void> {
  return http.put('/profile/password', payload, {
    skipAuthRefresh: true,
    headers: { 'X-Refresh-Token': getRefreshToken() }
  })
}

/** P-M04 完成新手引导（幂等） */
export function finishGuide(): Promise<void> {
  return http.post('/profile/guide', undefined, { silent: true })
}

/** P-M05 上传头像（JPG/PNG，服务端居中裁剪 200×200） */
export function uploadAvatar(file: File): Promise<{ url: string }> {
  const data = new FormData()
  data.append('file', file)
  return http.post('/files/avatar', data)
}
