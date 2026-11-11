import { http } from '@/api/request'

export function uploadImage(file: File): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/files/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    // 由设置页统一给出可操作的失败提示
    silent: true
  })
}
