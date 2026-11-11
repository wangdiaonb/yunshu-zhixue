import type { Directive } from 'vue'
import { usePermissionStore } from '@/store/permission'

type PermissionValue = string | string[]

/**
 * 按钮级权限：无权限时移除元素。
 *
 * <pre>{@code <el-button v-permission="'question:audit'">审核</el-button>}</pre>
 */
export const permissionDirective: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    const permissionStore = usePermissionStore()
    if (!permissionStore.hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
