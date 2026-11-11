<script setup lang="ts">
import { useRoute } from 'vue-router'
import PageShell from '@/components/PageShell.vue'

const route = useRoute()
</script>

<template>
  <PageShell
    title="面试报告"
    :description="`面试记录 ID：${route.params.id}；展示综合评分、六维能力雷达与逐题评语，报告仅在线查看。`"
  />
</template>
