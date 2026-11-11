import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginByPassword, loginBySms, logout as logoutApi, register } from '@/api/auth'
import { getProfile } from '@/api/profile'
import type {
  LoginResult,
  PasswordLoginPayload,
  PortalProfile,
  PortalUser,
  RegisterPayload,
  SmsLoginPayload
} from '@/types/auth'
import { clearTokens, getAccessToken, saveTokens } from '@/utils/token'

/**
 * 学员登录态：令牌为唯一登录凭据，资料按需拉取（P-M01）。
 */
export const useUserStore = defineStore('user', () => {
  const accessToken = ref(getAccessToken())
  const user = ref<PortalUser | null>(null)
  const profile = ref<PortalProfile | null>(null)
  const profileLoaded = ref(false)

  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const displayName = computed(() => user.value?.nickname ?? profile.value?.nickname ?? '学员')

  async function loginPassword(payload: PasswordLoginPayload, remember: boolean): Promise<void> {
    applyLogin(await loginByPassword(payload), remember)
  }

  async function loginSms(payload: SmsLoginPayload, remember: boolean): Promise<void> {
    applyLogin(await loginBySms(payload), remember)
  }

  async function registerAccount(payload: RegisterPayload, remember: boolean): Promise<void> {
    applyLogin(await register(payload), remember)
  }

  function applyLogin(result: LoginResult, remember: boolean): void {
    saveTokens(result.accessToken, result.refreshToken, remember)
    accessToken.value = result.accessToken
    user.value = result.student ?? null
    profile.value = null
    profileLoaded.value = false
  }

  /** 应用启动时恢复登录态并刷新资料；失败不阻断页面渲染 */
  async function bootstrap(): Promise<void> {
    if (!accessToken.value || profileLoaded.value) {
      return
    }
    try {
      await fetchProfile()
    } catch {
      // 401 由请求层统一清理登录态，这里只保证页面可继续渲染
    }
  }

  async function fetchProfile(): Promise<PortalProfile> {
    const data = await getProfile()
    profile.value = data
    user.value = { id: data.id, nickname: data.nickname, avatar: data.avatar }
    profileLoaded.value = true
    return data
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } finally {
      reset()
    }
  }

  function reset(): void {
    clearTokens()
    accessToken.value = ''
    user.value = null
    profile.value = null
    profileLoaded.value = false
  }

  function updateCachedUser(patch: Partial<PortalUser>): void {
    if (user.value) {
      user.value = { ...user.value, ...patch }
    }
    if (profile.value) {
      profile.value = { ...profile.value, ...patch }
    }
  }

  return {
    accessToken,
    user,
    profile,
    isLoggedIn,
    displayName,
    loginPassword,
    loginSms,
    registerAccount,
    bootstrap,
    fetchProfile,
    logout,
    reset,
    updateCachedUser
  }
})
