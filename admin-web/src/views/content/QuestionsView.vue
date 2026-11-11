<script setup lang="ts">
import { Download, Plus, Upload } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell
    title="题库管理"
    description="题目增删改查、审核流转、上下架与 Excel 导入导出；编辑后保持原状态（Q2）。"
  >
    <template #actions>
      <el-button v-permission="'question:import'" :icon="Upload">导入</el-button>
      <el-button v-permission="'question:export'" :icon="Download">导出</el-button>
      <el-button v-permission="'question:add'" type="primary" :icon="Plus">新增题目</el-button>
    </template>
  </PageShell>
</template>
