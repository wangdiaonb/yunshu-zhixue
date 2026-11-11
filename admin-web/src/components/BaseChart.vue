<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = withDefaults(
  defineProps<{
    option: EChartsOption
    height?: string
  }>(),
  { height: '320px' }
)

const container = ref<HTMLDivElement>()
const chart = shallowRef<echarts.ECharts>()
let observer: ResizeObserver | undefined

onMounted(() => {
  if (!container.value) {
    return
  }
  chart.value = echarts.init(container.value)
  chart.value.setOption(props.option)
  observer = new ResizeObserver(() => chart.value?.resize())
  observer.observe(container.value)
})

watch(
  () => props.option,
  (option) => chart.value?.setOption(option, true),
  { deep: true }
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart.value?.dispose()
})
</script>

<template>
  <div ref="container" class="base-chart" :style="{ height }" />
</template>

<style scoped>
.base-chart {
  width: 100%;
}
</style>
