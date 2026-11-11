<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell
    title="学习路线"
    description="维护路线与课程顺序、发布与停用；路线引用的课程不可删除（30007）。"
  >
    <template #actions>
      <el-button v-permission="'route:add'" type="primary" :icon="Plus">新增路线</el-button>
    </template>
  </PageShell>
</template>
