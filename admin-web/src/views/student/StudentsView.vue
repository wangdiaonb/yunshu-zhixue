<script setup lang="ts">
import { Download } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell
    title="学员管理"
    description="学员列表与详情（学习数据 / 已学课程 / 面试记录）、禁用与导出。"
  >
    <template #actions>
      <el-button v-permission="'student:view'" :icon="Download">导出</el-button>
    </template>
  </PageShell>
</template>
