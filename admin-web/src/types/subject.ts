/** 学科与阶段管理类型（对齐 04.API 接口设计 §5.3） */

export interface SubjectStats {
  totalSubjects: number
  subjectsWithStages: number
  totalStages: number
  totalQuestions: number
}

export interface SubjectOption {
  id: number
  name: string
  icon: string
}

export interface StageItem {
  id: number
  name: string
  icon: string | null
  sortOrder: number
  status: number
  questionCount: number
}

export interface SubjectCard {
  id: number
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
  courseCount: number
  routeCount: number
  questionCount: number
  stages: StageItem[]
}

export interface SubjectSavePayload {
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
}

export interface StageSavePayload {
  name: string
  icon?: string
  sortOrder?: number
}
