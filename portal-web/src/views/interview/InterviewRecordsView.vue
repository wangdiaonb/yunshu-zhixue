<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="面试记录"
    icon="🗂️"
    description="查看历史面试结果，对比每次表现，定位需要补强的能力方向。"
    :features="['面试次数与平均分', '历史成绩趋势', '报告回看', '再来一次']"
  />
</template>
