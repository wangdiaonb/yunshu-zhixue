/** 管理端认证契约（API 设计 §5.1） */
export interface AdminUser {
  id: number
  username: string
  roleId?: number
  roleCode?: string
  roleName?: string
  avatar?: string | null
}

export interface TokenPair {
  accessToken: string
  refreshToken: string
}

export interface AdminSession {
  adminUser: AdminUser
  /** 权限码集合（9 组 34 项，前端仅用于菜单/按钮渲染） */
  permissions: string[]
}

export interface AdminLoginResult extends TokenPair, AdminSession {}

export interface AdminLoginPayload {
  /** 用户名或手机号（Q4） */
  account: string
  password: string
}
