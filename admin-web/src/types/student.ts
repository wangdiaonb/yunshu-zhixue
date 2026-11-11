/** 学员管理类型（对齐 04.API 接口设计 §5.8） */

export interface StudentItem {
  id: number
  nickname: string
  avatar: string | null
  mobile: string
  email: string | null
  /** 1 已启用 / 0 已禁用 */
  status: number
  createdAt: string | null
  lastLoginAt: string | null
  /** 学习天数（有刷题记录的自然日数） */
  studyDays: number
  /** 已学课程数（路线课程完成数） */
  courseCount: number
  practiceCount: number
  /** 正确率（百分比，保留 1 位小数） */
  accuracy: number
  interviewCount: number
}

export interface StudentQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export interface StudentDetail {
  id: number
  nickname: string
  avatar: string | null
  mobile: string
  email: string | null
  /** 0 保密 / 1 男 / 2 女 */
  gender: number
  status: number
  createdAt: string | null
  lastLoginAt: string | null
  studyDays: number
  courseCount: number
  practiceCount: number
  correctCount: number
  accuracy: number
  interviewCount: number
}

export interface StudentInterviewRecord {
  id: number
  subjectId: number | null
  subjectName: string | null
  /** 1 简单 / 2 中等 / 3 困难 */
  difficulty: number
  questionCount: number
  /** 综合评分 0.0~10.0 */
  overallScore: number | null
  /** 累计时长（秒） */
  totalDuration: number | null
  startedAt: string | null
  completedAt: string | null
}
