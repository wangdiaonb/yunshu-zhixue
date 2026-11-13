<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="面试报告"
    icon="📊"
    description="综合评分、六维能力分析与逐题点评，帮助你明确下一步的补强方向。"
    :features="['综合评分与趋势', '六维能力分析', '优点与待提升项', '改进建议与推荐练习']"
  />
</template>
