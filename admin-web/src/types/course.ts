/** B 站课程库类型（对齐 04.API 接口设计 §5.4） */

export interface CourseStats {
  totalCourses: number
  totalHours: number
  relatedSubjectCount: number
}

export interface CourseItem {
  id: number
  name: string
  coverUrl: string | null
  durationMinutes: number
  source: number
  link: string | null
  bvid: string | null
  subjectId: number | null
  subjectName: string
  subjectIds: number[]
  subjectNames: string[]
  createdAt: string | null
}

export interface CourseQuery {
  page?: number
  pageSize?: number
  keyword?: string
  subjectId?: number
  source?: number
}

export interface CourseSavePayload {
  name: string
  coverUrl?: string
  durationMinutes: number
  source: number
  link?: string
  bvid?: string
  subjectId?: number
  subjectIds?: number[]
}

export interface CourseImportRow {
  rowNumber: number
  name: string
  subjectId: number | null
  subjectIds: number[]
  subjectNames: string[]
  durationMinutes: number
  source: number
  bvid: string | null
  link: string | null
  coverUrl: string | null
  warnings: string[]
}

export interface CourseImportErrorRow {
  rowNumber: number
  name: string
  messages: string[]
}

export interface CourseImportPreview {
  token: string | null
  total: number
  validCount: number
  invalidCount: number
  rows: CourseImportRow[]
  errors: CourseImportErrorRow[]
}
