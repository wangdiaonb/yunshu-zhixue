import { http } from '@/api/request'
import type { PageResult } from '@/types/api'
import type {
  CourseImportPreview,
  CourseItem,
  CourseQuery,
  CourseSavePayload,
  CourseStats
} from '@/types/course'

export function getCourseStats(): Promise<CourseStats> {
  return http.get('/courses/stats')
}

export function pageCourses(query: CourseQuery): Promise<PageResult<CourseItem>> {
  return http.get('/courses', { params: query })
}

export function createCourse(payload: CourseSavePayload): Promise<number> {
  return http.post('/courses', payload)
}

export function updateCourse(id: number, payload: CourseSavePayload): Promise<void> {
  return http.put(`/courses/${id}`, payload)
}

export function deleteCourse(id: number): Promise<void> {
  return http.delete(`/courses/${id}`)
}

export function downloadCourseImportTemplate(): Promise<Blob> {
  return http.get('/courses/import/template', { responseType: 'blob' })
}

export function previewCourseImport(file: File): Promise<CourseImportPreview> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/courses/import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60_000
  })
}

export function confirmCourseImport(token: string): Promise<{ imported: number }> {
  return http.post('/courses/import/confirm', { token })
}
