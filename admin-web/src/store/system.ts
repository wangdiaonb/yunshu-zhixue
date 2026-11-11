import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSettings } from '@/api/system/setting'
import type { SystemSetting } from '@/types/system'

const DEFAULT_SETTINGS: SystemSetting = {
  siteName: '云枢智学',
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: ''
}

/**
 * 平台基本信息：登录后加载一次，供侧边栏 Logo/名称等全局区域复用。
 */
export const useSystemSettingStore = defineStore('systemSetting', () => {
  const settings = ref<SystemSetting>({ ...DEFAULT_SETTINGS })
  const loaded = ref(false)
  const loading = ref(false)

  async function ensureLoaded(force = false): Promise<void> {
    if (loading.value || (loaded.value && !force)) {
      return
    }
    loading.value = true
    try {
      apply(await getSettings())
    } finally {
      loading.value = false
    }
  }

  /** 设置页保存/重置后同步到全局，侧边栏无需刷新即可更新。 */
  function apply(next: SystemSetting): void {
    settings.value = { ...next }
    loaded.value = true
  }

  function clear(): void {
    settings.value = { ...DEFAULT_SETTINGS }
    loaded.value = false
  }

  return { settings, loaded, loading, ensureLoaded, apply, clear }
})
