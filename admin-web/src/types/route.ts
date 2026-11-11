/** 学习路线类型（对齐 04.API 接口设计 §5.7） */

export interface RoutePreviewCourse {
  routeCourseId: number
  courseId: number
  name: string
  bvid: string | null
  coverUrl: string | null
  durationMinutes: number
  sortOrder: number
}

export interface RouteCard {
  id: number
  name: string
  subjectId: number
  subjectName: string
  subjectIcon: string
  icon: string | null
  targetEducation: string | null
  description: string | null
  status: number
  courseCount: number
  totalDurationMinutes: number
  previewCourses: RoutePreviewCourse[]
}

export interface RouteCourseItem {
  routeCourseId: number
  courseId: number
  name: string
  bvid: string | null
  coverUrl: string | null
  durationMinutes: number
  source: number
  sortOrder: number
  subjectId: number | null
  subjectName: string
  localSubject: boolean
}

export interface RouteDetail {
  id: number
  name: string
  subjectId: number
  subjectName: string
  subjectIcon: string
  icon: string | null
  targetEducation: string | null
  description: string | null
  status: number
  courseCount: number
  totalDurationMinutes: number
  courses: RouteCourseItem[]
}

export interface RouteCourseOption {
  courseId: number
  name: string
  bvid: string | null
  coverUrl: string | null
  durationMinutes: number
  source: number
  subjectId: number | null
  subjectName: string
  localSubject: boolean
}

export interface RouteQuery {
  keyword?: string
  subjectId?: number
  status?: number
}

export interface RouteSavePayload {
  name: string
  subjectId: number
  targetEducation?: string
  icon?: string
  description?: string
  status?: number
}
