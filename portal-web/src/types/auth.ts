/** 学员端认证与资料相关契约（API 设计 §4.1 / §4.8） */

export interface PortalUser {
  id: number
  nickname: string
  avatar?: string | null
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
}

export interface LoginResult extends TokenPair {
  student: PortalUser
}

export interface PasswordLoginPayload {
  /** 手机号或邮箱（不支持昵称登录） */
  account: string
  password: string
}

export interface SmsLoginPayload {
  mobile: string
  code: string
}

export interface SmsCodePayload {
  mobile: string
  type: 'register' | 'login' | 'reset'
}

export interface RegisterPayload {
  mobile: string
  code: string
  password: string
}

export interface ResetVerifyPayload {
  mobile: string
  code: string
}

export interface ResetVerifyResult {
  resetToken: string
}

export interface ResetPasswordPayload {
  resetToken: string
  newPassword: string
}

export interface ProfileStats {
  practiceCount: number
  interviewCount: number
  wrongQuestionCount: number
}

export interface ProfileStudy {
  totalDays: number
  accuracyRate: number
}

export interface PortalProfile {
  id: number
  nickname: string
  mobile: string
  email?: string | null
  gender: number
  preferredSubjectId?: number | null
  avatar?: string | null
  guideFinished: number
  stats: ProfileStats
  study: ProfileStudy
}
