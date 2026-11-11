<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell
    title="B站课程库"
    description="维护外链课程与学科关联，支持「解析预览 → 校验报告 → 确认入库」两段式批量导入。"
  >
    <template #actions>
      <el-button v-permission="'course:add'" type="primary" :icon="Plus">新增课程</el-button>
    </template>
  </PageShell>
</template>
