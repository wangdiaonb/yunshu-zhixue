import { http } from '@/api/request'
import type {
  PermissionNode,
  RoleCard,
  RoleCreatePayload
} from '@/types/system'

export function listRoles(): Promise<RoleCard[]> {
  return http.get('/roles')
}

export function createRole(payload: RoleCreatePayload): Promise<number> {
  return http.post('/roles', payload)
}

export function deleteRole(id: number): Promise<void> {
  return http.delete(`/roles/${id}`)
}

export function getPermissionTree(roleId: number): Promise<PermissionNode[]> {
  return http.get('/permissions/tree', { params: { roleId } })
}

export function saveRolePermissions(id: number, permissionIds: number[]): Promise<void> {
  return http.put(`/roles/${id}/permissions`, { permissionIds })
}
