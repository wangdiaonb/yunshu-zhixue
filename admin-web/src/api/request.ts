import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, FieldErrorItem } from '@/types/api'
import type { TokenPair } from '@/types/auth'
import { clearSession, getAccessToken, getRefreshToken, updateTokens } from '@/utils/token'

declare module 'axios' {
  interface AxiosRequestConfig {
    silent?: boolean
    skipAuthRefresh?: boolean
  }
}

export class ApiError extends Error {
  readonly code: number
  readonly fieldErrors?: FieldErrorItem[]

  constructor(code: number, message: string, fieldErrors?: FieldErrorItem[]) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const baseURL = import.meta.env.VITE_API_BASE_URL

const client = axios.create({
  baseURL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' }
})

const refreshClient = axios.create({
  baseURL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' }
})

let refreshing: Promise<void> | null = null

client.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown> | undefined
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      throw new ApiError(body.code, body.message, body.fieldErrors ?? undefined)
    }
    return response.data
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config
    const status = error.response?.status

    if (status === 401 && config && !config.skipAuthRefresh && !isAuthEndpoint(config.url)) {
      try {
        await refreshAccessToken()
        return client.request<unknown, unknown>(config)
      } catch {
        handleSessionExpired()
        throw new ApiError(10001, '登录状态已失效，请重新登录')
      }
    }

    const apiError = toApiError(error)
    if (!config?.silent) {
      ElMessage.error(apiError.message)
    }
    throw apiError
  }
)

function isAuthEndpoint(url?: string): boolean {
  return Boolean(url && (url.includes('/auth/login') || url.includes('/auth/refresh')))
}

function refreshAccessToken(): Promise<void> {
  if (refreshing) {
    return refreshing
  }
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('缺少刷新令牌'))
  }
  refreshing = refreshClient
    .post<ApiResponse<TokenPair>>('/auth/refresh', null, {
      headers: { 'X-Refresh-Token': refreshToken }
    })
    .then(({ data }) => {
      if (data.code !== 0) {
        throw new Error(data.message)
      }
      updateTokens(data.data.accessToken, data.data.refreshToken)
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

function handleSessionExpired(): void {
  clearSession()
  if (!window.location.pathname.endsWith('/login')) {
    const base = import.meta.env.BASE_URL.endsWith('/')
      ? import.meta.env.BASE_URL
      : `${import.meta.env.BASE_URL}/`
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.assign(`${base}login?redirect=${redirect}`)
  }
}

function toApiError(error: AxiosError<ApiResponse<unknown>>): ApiError {
  const body = error.response?.data
  if (body && typeof body.code === 'number') {
    return new ApiError(body.code, body.message, body.fieldErrors ?? undefined)
  }
  if (error.code === 'ECONNABORTED') {
    return new ApiError(-1, '请求超时，请稍后重试')
  }
  if (!error.response) {
    return new ApiError(-1, '网络异常，请检查网络后重试')
  }
  return new ApiError(error.response.status, '请求失败，请稍后重试')
}

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  // 响应拦截器已解包统一响应体，此处以业务 DTO 类型返回
  return client.request(config) as unknown as Promise<T>
}

export const http = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, url, method: 'GET' })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, url, data, method: 'POST' })
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, url, data, method: 'PUT' })
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, url, method: 'DELETE' })
  }
}
