<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell
    title="学科管理"
    description="维护学科与阶段：排序、启停、关联内容统计；阶段权限随学科权限点（D4-5）。"
  >
    <template #actions>
      <el-button v-permission="'subject:add'" type="primary" :icon="Plus">新增学科</el-button>
    </template>
  </PageShell>
</template>
