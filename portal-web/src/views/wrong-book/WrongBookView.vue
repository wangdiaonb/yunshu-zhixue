<script setup lang="ts">
import PortalPlaceholder from '@/components/PortalPlaceholder.vue'
</script>

<template>
  <PortalPlaceholder
    title="错题本"
    icon="📕"
    description="按错误次数分级归集错题，连续答对 3 次自动标记为已掌握，支持批量整理与重做。"
    :features="['高频错题 Top3', '错误次数分级', '错题重做与纠正进度', '批量标记已掌握 / 移除']"
  />
</template>
