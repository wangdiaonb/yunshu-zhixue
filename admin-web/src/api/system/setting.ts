import { http } from '@/api/request'
import type { SystemSetting } from '@/types/system'

export function getSettings(): Promise<SystemSetting> {
  return http.get('/settings')
}

export function updateSettings(payload: SystemSetting): Promise<SystemSetting> {
  return http.put('/settings', payload)
}

export function resetSettings(): Promise<SystemSetting> {
  return http.post('/settings/reset')
}
