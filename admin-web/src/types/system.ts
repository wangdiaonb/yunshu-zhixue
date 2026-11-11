/** 系统管理模块接口类型（对齐 04.API 接口设计 §5.9 ~ §5.12） */

export interface AdminUserItem {
  id: number
  username: string
  mobile: string | null
  email: string | null
  roleId: number
  roleName: string | null
  roleCode: string | null
  status: number
  isBuiltin: number
  lastLoginAt: string | null
  createdAt: string | null
}

export interface AdminUserQuery {
  page?: number
  pageSize?: number
  keyword?: string
  roleId?: number
  status?: number
}

export interface AdminUserCreatePayload {
  username: string
  mobile: string
  email?: string | null
  roleId: number
  password: string
  status: number
}

export interface AdminUserUpdatePayload {
  username: string
  mobile: string
  email?: string | null
  roleId: number
  password?: string
  status: number
}

export interface OperationLogItem {
  id: number
  userId: number
  username: string
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string | null
}

export interface AdminUserDetail {
  user: AdminUserItem
  recentLogs: OperationLogItem[]
}

export interface RoleOption {
  id: number
  name: string
  code: string
}

export interface RolePermissionTag {
  id: number
  code: string
  name: string
  groupName: string
}

export interface RoleCard {
  id: number
  name: string
  code: string
  description: string | null
  userCount: number
  permissions: RolePermissionTag[]
  totalPermissionCount: number
  superAdmin: boolean
}

export interface RoleCreatePayload {
  name: string
  code: string
  description?: string
}

export interface PermissionNode {
  id: number
  name: string
  code: string
  checked: boolean
  children: PermissionNode[]
}

export interface OperationLogQuery {
  page?: number
  pageSize?: number
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
}

export interface SystemSetting {
  siteName: string
  siteDescription: string
  logoUrl: string
  defaultAvatar: string
}
