import { http } from '@/api/request'
import type { PageResult } from '@/types/api'
import type {
  InterviewImportPreview,
  InterviewQuestionCreatePayload,
  InterviewQuestionDetail,
  InterviewQuestionItem,
  InterviewQuestionQuery,
  InterviewQuestionUpdatePayload,
  InterviewTopicItem,
  InterviewTopicSavePayload
} from '@/types/interview-bank'

export function pageInterviewQuestions(
  query: InterviewQuestionQuery
): Promise<PageResult<InterviewQuestionItem>> {
  return http.get('/interview-questions', { params: query })
}

export function getInterviewQuestion(id: number): Promise<InterviewQuestionDetail> {
  return http.get(`/interview-questions/${id}`)
}

export function createInterviewQuestion(payload: InterviewQuestionCreatePayload): Promise<number> {
  return http.post('/interview-questions', payload)
}

export function updateInterviewQuestion(
  id: number,
  payload: InterviewQuestionUpdatePayload
): Promise<void> {
  return http.put(`/interview-questions/${id}`, payload)
}

export function deleteInterviewQuestion(id: number): Promise<void> {
  return http.delete(`/interview-questions/${id}`)
}

export function changeInterviewQuestionStatus(id: number, status: number): Promise<void> {
  return http.post(`/interview-questions/${id}/status`, { status })
}

export function downloadInterviewTemplate(): Promise<Blob> {
  return http.get('/interview-questions/import/template', { responseType: 'blob' })
}

export function previewInterviewImport(
  file: File,
  defaultSubjectId?: number
): Promise<InterviewImportPreview> {
  const formData = new FormData()
  formData.append('file', file)
  if (defaultSubjectId) {
    formData.append('defaultSubjectId', String(defaultSubjectId))
  }
  return http.post('/interview-questions/import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60_000
  })
}

export function confirmInterviewImport(token: string): Promise<{ imported: number }> {
  return http.post('/interview-questions/import/confirm', { token })
}

export function listInterviewTopics(subjectId?: number): Promise<InterviewTopicItem[]> {
  return http.get('/interview-topics', { params: { subjectId } })
}

export function createInterviewTopic(payload: InterviewTopicSavePayload): Promise<number> {
  return http.post('/interview-topics', payload)
}

export function updateInterviewTopic(
  id: number,
  payload: InterviewTopicSavePayload
): Promise<void> {
  return http.put(`/interview-topics/${id}`, payload)
}

export function deleteInterviewTopic(id: number): Promise<void> {
  return http.delete(`/interview-topics/${id}`)
}
