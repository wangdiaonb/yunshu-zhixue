/** 题库管理类型（对齐 04.API 接口设计 §5.5） */

export interface QuestionStats {
  total: number
  published: number
  pending: number
  single: number
  multiple: number
  judge: number
  shortAnswer: number
}

export interface QuestionOptionItem {
  id: number
  key: string
  text: string
  correct: boolean
  sortOrder: number
}

export interface QuestionItem {
  id: number
  content: string
  type: number
  difficulty: number
  status: number
  stageId: number
  stageName: string
  subjectId: number | null
  subjectName: string
  tags: string | null
  rejectReason: string | null
  createdAt: string | null
  doneCount: number
  accuracyRate: number
}

export interface QuestionDetail extends QuestionItem {
  answer: string | null
  analysis: string | null
  options: QuestionOptionItem[]
  correctCount: number
  updatedAt: string | null
}

export interface QuestionQuery {
  page?: number
  pageSize?: number
  keyword?: string
  subjectId?: number
  stageId?: number
  type?: number
  difficulty?: number
  status?: number
}

export interface QuestionOptionPayload {
  text: string
  correct: boolean
}

export interface QuestionSavePayload {
  content: string
  stageId: number
  type: number
  difficulty: number
  tags?: string
  analysis?: string
  answer?: string
  options?: QuestionOptionPayload[]
}

export interface QuestionImportOption {
  key: string
  text: string
  correct: boolean
}

export interface QuestionImportRow {
  rowNumber: number
  content: string
  stageName: string
  type: number
  difficulty: number
  options: QuestionImportOption[]
  answer: string | null
  analysis: string | null
  tags: string | null
}

export interface QuestionImportErrorRow {
  rowNumber: number
  content: string
  messages: string[]
}

export interface QuestionImportPreview {
  token: string | null
  total: number
  validCount: number
  invalidCount: number
  subjectId: number
  subjectName: string
  stagesToCreate: string[]
  rows: QuestionImportRow[]
  errors: QuestionImportErrorRow[]
}
