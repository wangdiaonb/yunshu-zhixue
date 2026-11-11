import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 响应式媒体查询：用于侧边栏桌面折叠 / 移动端抽屉切换。
 */
export function useMediaQuery(query: string) {
  const matches = ref(typeof window !== 'undefined' && window.matchMedia(query).matches)
  let mediaQuery: MediaQueryList | undefined

  function update(event: MediaQueryListEvent | MediaQueryList): void {
    matches.value = event.matches
  }

  onMounted(() => {
    mediaQuery = window.matchMedia(query)
    update(mediaQuery)
    mediaQuery.addEventListener('change', update)
  })

  onBeforeUnmount(() => {
    mediaQuery?.removeEventListener('change', update)
  })

  return matches
}
