/**
 * 令牌存储：勾选「记住我」存 localStorage，否则存 sessionStorage（R-04，仅存储策略差异）。
 * 两端使用不同键名，令牌互不通用（D12）。
 */
const ACCESS_KEY = 'yszx_portal_access_token'
const REFRESH_KEY = 'yszx_portal_refresh_token'

function read(key: string): string {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key) ?? ''
}

export function getAccessToken(): string {
  return read(ACCESS_KEY)
}

export function getRefreshToken(): string {
  return read(REFRESH_KEY)
}

export function saveTokens(accessToken: string, refreshToken: string, remember: boolean): void {
  const storage = remember ? localStorage : sessionStorage
  const other = remember ? sessionStorage : localStorage
  other.removeItem(ACCESS_KEY)
  other.removeItem(REFRESH_KEY)
  storage.setItem(ACCESS_KEY, accessToken)
  storage.setItem(REFRESH_KEY, refreshToken)
}

/** 刷新令牌后原地更新，保持原有存储位置（不改变「记住我」选择）。 */
export function updateTokens(accessToken: string, refreshToken: string): void {
  const storage = localStorage.getItem(ACCESS_KEY) ? localStorage : sessionStorage
  storage.setItem(ACCESS_KEY, accessToken)
  storage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens(): void {
  for (const storage of [localStorage, sessionStorage]) {
    storage.removeItem(ACCESS_KEY)
    storage.removeItem(REFRESH_KEY)
  }
}

export function hasToken(): boolean {
  return Boolean(getAccessToken())
}
