import { http } from '@/api/request'
import type { PageResult } from '@/types/api'
import type { OperationLogItem, OperationLogQuery } from '@/types/system'

export function pageLogs(params: OperationLogQuery): Promise<PageResult<OperationLogItem>> {
  return http.get('/logs', { params })
}

export function getLog(id: number): Promise<OperationLogItem> {
  return http.get(`/logs/${id}`)
}

export function exportLogs(params: OperationLogQuery): Promise<Blob> {
  return http.get('/logs/export', { params, responseType: 'blob' })
}

export function clearLogs(): Promise<void> {
  return http.delete('/logs')
}
