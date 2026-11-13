<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    /** 为 null 时展示占位符，接口返回后再播放递增动画 */
    value: number | null
    suffix?: string
    decimals?: number
    duration?: number
  }>(),
  {
    suffix: '',
    decimals: 0,
    duration: 1200
  }
)

const displayed = ref<number | null>(null)
let frame = 0

watch(
  () => props.value,
  (target) => {
    cancelAnimationFrame(frame)
    if (typeof target !== 'number' || Number.isNaN(target)) {
      displayed.value = null
      return
    }

    const from = displayed.value ?? 0
    const startTime = performance.now()
    const step = (now: number) => {
      const progress = Math.min((now - startTime) / props.duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      displayed.value = from + (target - from) * eased
      if (progress < 1) {
        frame = requestAnimationFrame(step)
      } else {
        displayed.value = target
      }
    }
    frame = requestAnimationFrame(step)
  },
  { immediate: true }
)

onBeforeUnmount(() => cancelAnimationFrame(frame))

const text = computed(() => {
  if (displayed.value === null) {
    return '--'
  }
  const number = displayed.value.toLocaleString('zh-CN', {
    minimumFractionDigits: props.decimals,
    maximumFractionDigits: props.decimals
  })
  return `${number}${props.suffix}`
})
</script>

<template>
  <span class="stat-counter">{{ text }}</span>
</template>

<style scoped>
.stat-counter {
  font-variant-numeric: tabular-nums;
}
</style>
