<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="刷题练习"
    icon="✏️"
    description="顺序或随机练习，客观题即时判分，简答题由 AI 批改（≥6 分通过），练习记录自动沉淀。"
    :features="['顺序 / 随机练习模式', '答题卡与计时器', '客观题即时判分', '简答题 AI 批改', '练习进度本地保存']"
  />
</template>
