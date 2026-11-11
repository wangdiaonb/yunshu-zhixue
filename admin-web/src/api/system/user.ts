import { http } from '@/api/request'
import type { PageResult } from '@/types/api'
import type {
  AdminUserCreatePayload,
  AdminUserDetail,
  AdminUserItem,
  AdminUserQuery,
  AdminUserUpdatePayload,
  RoleOption
} from '@/types/system'

export function pageUsers(params: AdminUserQuery): Promise<PageResult<AdminUserItem>> {
  return http.get('/users', { params })
}

export function listRoleOptions(): Promise<RoleOption[]> {
  return http.get('/users/role-options')
}

export function getUserDetail(id: number): Promise<AdminUserDetail> {
  return http.get(`/users/${id}/detail`)
}

export function createUser(payload: AdminUserCreatePayload): Promise<number> {
  return http.post('/users', payload)
}

export function updateUser(id: number, payload: AdminUserUpdatePayload): Promise<void> {
  return http.put(`/users/${id}`, payload)
}

export function deleteUser(id: number): Promise<void> {
  return http.delete(`/users/${id}`)
}

export function batchUpdateUserStatus(ids: number[], status: number): Promise<void> {
  return http.put('/users/batch-status', { ids, status })
}

export function batchDeleteUsers(ids: number[]): Promise<void> {
  return http.delete('/users/batch', { data: { ids } })
}

export function resetUserPassword(id: number): Promise<{ password: string }> {
  return http.post(`/users/${id}/reset-password`)
}
