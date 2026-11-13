import { http } from './request'
import type { SiteSettings } from '@/types/site'

/** P-H04 站点设置（公开，来源为管理端系统设置） */
export function getSiteSettings(): Promise<SiteSettings> {
  return http.get('/site/settings', { silent: true, skipAuthRefresh: true })
}
