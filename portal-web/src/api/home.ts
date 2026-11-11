import { http } from './request'

export interface PlatformStats {
  interviewQuestionCount: number
  studentCount: number
  interviewCount: number
  /** 历史 AI 面试 overall_score >= 8 的占比（D4-3） */
  goodRate: number
}

/** P-H01 平台数据（公开） */
export function getPlatformStats(): Promise<PlatformStats> {
  return http.get('/home/platform-stats', { silent: true })
}
