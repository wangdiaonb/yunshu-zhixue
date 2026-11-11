import type { AdminSession } from '@/types/auth'

/**
 * 管理端登录态存储：键名与学员端完全隔离（D12）。
 * 权限码仅用于前端菜单与按钮显隐，接口鉴权始终以后端为准。
 */
const ACCESS_KEY = 'yszx_admin_access_token'
const REFRESH_KEY = 'yszx_admin_refresh_token'
const SESSION_KEY = 'yszx_admin_session'

function read(key: string): string {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key) ?? ''
}

function activeStorage(): Storage {
  return localStorage.getItem(ACCESS_KEY) ? localStorage : sessionStorage
}

export function getAccessToken(): string {
  return read(ACCESS_KEY)
}

export function getRefreshToken(): string {
  return read(REFRESH_KEY)
}

export function readSession(): AdminSession | null {
  const raw = read(SESSION_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AdminSession
  } catch {
    return null
  }
}

export function saveSession(
  accessToken: string,
  refreshToken: string,
  session: AdminSession,
  remember: boolean
): void {
  const storage = remember ? localStorage : sessionStorage
  const other = remember ? sessionStorage : localStorage
  for (const key of [ACCESS_KEY, REFRESH_KEY, SESSION_KEY]) {
    other.removeItem(key)
  }
  storage.setItem(ACCESS_KEY, accessToken)
  storage.setItem(REFRESH_KEY, refreshToken)
  storage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function updateTokens(accessToken: string, refreshToken: string): void {
  const storage = activeStorage()
  storage.setItem(ACCESS_KEY, accessToken)
  storage.setItem(REFRESH_KEY, refreshToken)
}

export function clearSession(): void {
  for (const storage of [localStorage, sessionStorage]) {
    storage.removeItem(ACCESS_KEY)
    storage.removeItem(REFRESH_KEY)
    storage.removeItem(SESSION_KEY)
  }
}

export function hasToken(): boolean {
  return Boolean(getAccessToken())
}
