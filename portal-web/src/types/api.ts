/** 统一响应体（API 设计 §2.3 / §2.4），业务 DTO 以 OpenAPI 生成类型为准 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  fieldErrors?: FieldErrorItem[] | null
  fileErrors?: FileRowError[] | null
}

export interface FieldErrorItem {
  field: string
  reason: string
}

export interface FileRowError {
  row: number
  field: string
  reason: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface PageQuery {
  page?: number
  pageSize?: number
}
