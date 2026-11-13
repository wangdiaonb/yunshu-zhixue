import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getSiteSettings } from '@/api/site'
import type { SiteSettings } from '@/types/site'

const DEFAULT_SITE_NAME = '云枢智学'
const DEFAULT_SITE_DESCRIPTION = 'AI 赋能的智能面试训练平台'

const DEFAULT_SETTINGS: SiteSettings = {
  siteName: DEFAULT_SITE_NAME,
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: ''
}

/**
 * 站点品牌：平台名称 / Logo / 描述来自管理端系统设置（P-H04）。
 * 接口未就绪或请求失败时回退到默认品牌，不阻断页面渲染。
 */
export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...DEFAULT_SETTINGS })
  const loaded = ref(false)
  const loading = ref(false)
  let pending: Promise<void> | null = null

  const siteName = computed(() => settings.value.siteName.trim() || DEFAULT_SITE_NAME)
  const siteDescription = computed(
    () => settings.value.siteDescription.trim() || DEFAULT_SITE_DESCRIPTION
  )
  const logoUrl = computed(() => settings.value.logoUrl.trim())
  const defaultAvatar = computed(() => settings.value.defaultAvatar.trim())

  async function ensureLoaded(force = false): Promise<void> {
    if (pending) {
      return pending
    }
    if (loaded.value && !force) {
      return
    }
    loading.value = true
    pending = getSiteSettings()
      .then((data) => {
        settings.value = { ...DEFAULT_SETTINGS, ...data }
        loaded.value = true
      })
      .catch(() => {
        // 品牌接口异常时保持默认值
      })
      .finally(() => {
        loading.value = false
        pending = null
      })
    return pending
  }

  return {
    settings,
    loaded,
    loading,
    siteName,
    siteDescription,
    logoUrl,
    defaultAvatar,
    ensureLoaded
  }
})
