import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { AdminSession, AdminUser, AdminLoginPayload } from '@/types/auth'
import { clearSession, getAccessToken, readSession, saveSession } from '@/utils/token'
import { usePermissionStore } from '@/store/permission'

/**
 * 管理员登录态：登录成功同时同步权限码到 permission store。
 */
export const useUserStore = defineStore('user', () => {
  const permissionStore = usePermissionStore()
  const session = ref<AdminSession | null>(readSession())
  const accessToken = ref(getAccessToken())

  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const adminUser = computed<AdminUser | null>(() => session.value?.adminUser ?? null)
  const displayName = computed(() => adminUser.value?.username ?? '管理员')

  async function login(payload: AdminLoginPayload, remember: boolean): Promise<void> {
    const result = await loginApi(payload)
    const next: AdminSession = {
      adminUser: result.adminUser,
      permissions: result.permissions ?? []
    }
    saveSession(result.accessToken, result.refreshToken, next, remember)
    accessToken.value = result.accessToken
    session.value = next
    permissionStore.applySession(next)
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      reset()
    }
  }

  function reset(): void {
    clearSession()
    accessToken.value = ''
    session.value = null
    permissionStore.clear()
  }

  return { session, accessToken, adminUser, displayName, isLoggedIn, login, logout, reset }
})
