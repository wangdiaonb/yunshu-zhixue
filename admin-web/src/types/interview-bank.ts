/** 面试题库管理类型（对齐 04.API 接口设计 §5.6） */

export interface InterviewQuestionItem {
  id: number
  subjectId: number
  subjectName: string
  content: string
  topic: string | null
  difficulty: number
  tags: string | null
  /** 来源：1 文档解析 / 2 手工录入 / 3 Excel 导入 */
  sourceType: number
  sourceDocName: string | null
  sourceSeq: number | null
  /** 状态：1 已上架 / 2 已下架 */
  status: number
  createdAt: string | null
}

export interface InterviewQuestionDetail extends InterviewQuestionItem {
  referenceAnswer: string | null
  updatedAt: string | null
}

export interface InterviewQuestionQuery {
  page?: number
  pageSize?: number
  keyword?: string
  subjectId?: number
  topicId?: number
  topic?: string
  difficulty?: number
  unclassified?: boolean
  status?: number
}

export interface InterviewQuestionCreatePayload {
  subjectId: number
  content: string
  referenceAnswer?: string
  topic?: string
  difficulty?: number
  tags?: string
}

export interface InterviewQuestionUpdatePayload {
  topic?: string | null
  difficulty: number
}

export interface InterviewTopicItem {
  id: number | null
  name: string
  subjectId: number | null
  subjectName: string | null
  sortOrder: number
  questionCount: number
  /** false 表示词表外主题（题库在用但未维护进词表） */
  inVocabulary: boolean
}

export interface InterviewTopicSavePayload {
  name: string
  subjectId?: number | null
  sortOrder?: number
}

export interface InterviewImportRow {
  rowNumber: number
  subjectId: number
  subjectName: string
  content: string
  topic: string | null
  difficulty: number
  tags: string | null
  referenceAnswer: string | null
}

export interface InterviewImportErrorRow {
  rowNumber: number
  content: string
  messages: string[]
}

export interface InterviewImportSubjectStat {
  subjectId: number
  subjectName: string
  count: number
}

export interface InterviewImportPreview {
  token: string | null
  total: number
  validCount: number
  invalidCount: number
  subjects: InterviewImportSubjectStat[]
  topicsNotInVocabulary: string[]
  rows: InterviewImportRow[]
  errors: InterviewImportErrorRow[]
}
