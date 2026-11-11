import { onBeforeUnmount, ref } from 'vue'

/**
 * 秒级倒计时：短信验证码 60s 冷却（D18）等场景使用。
 */
export function useCountdown(seconds = 60) {
  const remaining = ref(0)
  let timer: number | undefined

  function start(): void {
    if (remaining.value > 0) {
      return
    }
    remaining.value = seconds
    timer = window.setInterval(() => {
      remaining.value -= 1
      if (remaining.value <= 0) {
        stop()
      }
    }, 1000)
  }

  function stop(): void {
    if (timer) {
      window.clearInterval(timer)
      timer = undefined
    }
    remaining.value = 0
  }

  onBeforeUnmount(stop)

  return { remaining, start, stop }
}
