import { http } from '@/api/request'
import type { PageResult } from '@/types/api'
import type { StudentDetail, StudentInterviewRecord, StudentItem, StudentQuery } from '@/types/student'

export function pageStudents(query: StudentQuery): Promise<PageResult<StudentItem>> {
  return http.get('/students', { params: query })
}

export function getStudentDetail(id: number): Promise<StudentDetail> {
  return http.get(`/students/${id}/detail`)
}

export function pageStudentInterviews(
  id: number,
  page: number,
  pageSize: number
): Promise<PageResult<StudentInterviewRecord>> {
  return http.get(`/students/${id}/interviews`, { params: { page, pageSize } })
}

export function changeStudentStatus(id: number, status: number): Promise<void> {
  return http.put(`/students/${id}/status`, { status })
}

export function batchChangeStudentStatus(ids: number[], status: number): Promise<number> {
  return http.put('/students/batch-status', { ids, status })
}

export function deleteStudent(id: number): Promise<void> {
  return http.delete(`/students/${id}`)
}

export function batchDeleteStudents(ids: number[]): Promise<number> {
  return http.delete('/students/batch', { data: { ids } })
}
