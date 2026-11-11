import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AdminSession } from '@/types/auth'
import { readSession } from '@/utils/token'

/**
 * 权限上下文：按权限码控制菜单与按钮可见性（D4-5，9 组 34 项）。
 * 仅用于前端渲染，接口鉴权由后端 @RequirePermission 独立完成。
 */
export const usePermissionStore = defineStore('permission', () => {
  const cached = readSession()
  const codes = ref<string[]>(cached?.permissions ?? [])
  const roleCode = ref<string>(cached?.adminUser.roleCode ?? '')

  const isSuperAdmin = computed(() => roleCode.value === 'super_admin')

  /** 未声明权限码的页面/按钮对所有登录用户可见 */
  function hasPermission(code?: string | string[]): boolean {
    if (!code || (Array.isArray(code) && code.length === 0)) {
      return true
    }
    if (isSuperAdmin.value) {
      return true
    }
    const required = Array.isArray(code) ? code : [code]
    return required.some((item) => codes.value.includes(item))
  }

  function applySession(session: AdminSession): void {
    codes.value = [...(session.permissions ?? [])]
    roleCode.value = session.adminUser.roleCode ?? ''
  }

  function clear(): void {
    codes.value = []
    roleCode.value = ''
  }

  return { codes, roleCode, isSuperAdmin, hasPermission, applySession, clear }
})
