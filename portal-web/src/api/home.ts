import { http } from './request'

export interface PlatformStats {
  interviewQuestionCount: number
  studentCount: number
  interviewCount: number
  /** 历史 AI 面试 overall_score >= 8 的占比（D4-3），兼容 0~1 与 0~100 两种口径 */
  goodRate: number
}

export interface HotStage {
  stageId: number
  subjectId: number
  subjectName: string
  stageName: string
  questionCount: number
}

export interface LearningStats {
  consecutiveDays: number
  accuracyRate: number
  totalPracticeCount: number
}

export interface WeekTrendItem {
  /** yyyy-MM-dd */
  date: string
  count: number
}

export interface LearningOverview {
  learningStats: LearningStats
  /** 近 7 天练习量，按日期升序 */
  weekTrend: WeekTrendItem[]
  todayCount: number
  /** 1 表示已完成新手引导 */
  guideFinished: number
}

/** P-H01 平台数据（公开）：401 不触发全局登录失效跳转 */
export function getPlatformStats(): Promise<PlatformStats> {
  return http.get('/home/platform-stats', { silent: true, skipAuthRefresh: true })
}

/** P-H02 热门阶段（公开，按题目数倒序 Top N） */
export function getHotStages(): Promise<HotStage[]> {
  return http.get('/home/hot-stages', { silent: true, skipAuthRefresh: true })
}

/** P-H03 学习概览（登录） */
export function getLearningOverview(): Promise<LearningOverview> {
  return http.get('/home/learning-overview', { silent: true })
}
