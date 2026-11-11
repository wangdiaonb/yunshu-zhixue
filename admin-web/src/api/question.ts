import { http } from '@/api/request'
import type { PageResult } from '@/types/api'
import type {
  QuestionDetail,
  QuestionImportPreview,
  QuestionItem,
  QuestionQuery,
  QuestionSavePayload,
  QuestionStats
} from '@/types/question'

export function getQuestionStats(): Promise<QuestionStats> {
  return http.get('/questions/stats')
}

export function pageQuestions(query: QuestionQuery): Promise<PageResult<QuestionItem>> {
  return http.get('/questions', { params: query })
}

export function getQuestionDetail(id: number): Promise<QuestionDetail> {
  return http.get(`/questions/${id}`)
}

export function createQuestion(payload: QuestionSavePayload): Promise<number> {
  return http.post('/questions', payload)
}

export function updateQuestion(id: number, payload: QuestionSavePayload): Promise<void> {
  return http.put(`/questions/${id}`, payload)
}

export function deleteQuestion(id: number): Promise<{ refCount: number }> {
  return http.delete(`/questions/${id}`)
}

export function auditQuestion(
  id: number,
  payload: { pass: boolean; rejectReason?: string }
): Promise<void> {
  return http.post(`/questions/${id}/audit`, payload)
}

export function changeQuestionStatus(id: number, action: 'on' | 'off'): Promise<void> {
  return http.post(`/questions/${id}/status`, { action })
}

export function resubmitQuestion(id: number): Promise<void> {
  return http.post(`/questions/${id}/resubmit`)
}

export function batchQuestionStatus(
  ids: number[],
  action: 'on' | 'off'
): Promise<{ updatedCount: number; skippedCount: number }> {
  return http.post('/questions/batch-status', { ids, action })
}

export function batchDeleteQuestions(
  ids: number[]
): Promise<{ deletedCount: number; refCount: number }> {
  return http.post('/questions/batch-delete', { ids })
}

export function downloadQuestionTemplate(): Promise<Blob> {
  return http.get('/questions/import/template', { responseType: 'blob' })
}

export function previewQuestionImport(file: File, subjectId: number): Promise<QuestionImportPreview> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('subjectId', String(subjectId))
  return http.post('/questions/import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60_000
  })
}

export function confirmQuestionImport(
  token: string
): Promise<{ imported: number; stagesCreated: number }> {
  return http.post('/questions/import/confirm', { token })
}

export function exportQuestions(query: QuestionQuery): Promise<Blob> {
  return http.get('/questions/export', {
    params: query,
    responseType: 'blob',
    timeout: 60_000
  })
}
