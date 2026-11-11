import { http } from '@/api/request'
import type {
  StageSavePayload,
  SubjectCard,
  SubjectOption,
  SubjectSavePayload,
  SubjectStats
} from '@/types/subject'

export function getSubjectStats(): Promise<SubjectStats> {
  return http.get('/subjects/stats')
}

export function listSubjects(): Promise<SubjectCard[]> {
  return http.get('/subjects')
}

export function listSubjectOptions(): Promise<SubjectOption[]> {
  return http.get('/subjects/options')
}

export function createSubject(payload: SubjectSavePayload): Promise<number> {
  return http.post('/subjects', payload)
}

export function updateSubject(id: number, payload: SubjectSavePayload): Promise<void> {
  return http.put(`/subjects/${id}`, payload)
}

export function sortSubjects(orderedIds: number[]): Promise<void> {
  return http.put('/subjects/sort', { orderedIds })
}

export function deleteSubject(id: number): Promise<void> {
  return http.delete(`/subjects/${id}`)
}

export function createStage(subjectId: number, payload: StageSavePayload): Promise<number> {
  return http.post(`/subjects/${subjectId}/stages`, payload)
}

export function updateStage(
  subjectId: number,
  stageId: number,
  payload: StageSavePayload
): Promise<void> {
  return http.put(`/subjects/${subjectId}/stages/${stageId}`, payload)
}

export function sortStages(subjectId: number, orderedIds: number[]): Promise<void> {
  return http.put(`/subjects/${subjectId}/stages/sort`, { orderedIds })
}

export function updateStageStatus(subjectId: number, stageId: number, status: number): Promise<void> {
  return http.put(`/subjects/${subjectId}/stages/${stageId}/status`, { status })
}

export function deleteStage(subjectId: number, stageId: number): Promise<void> {
  return http.delete(`/subjects/${subjectId}/stages/${stageId}`)
}
