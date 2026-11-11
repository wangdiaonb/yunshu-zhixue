<script setup lang="ts">
import { Upload } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell
    title="面试题库管理"
    description="Markdown 文档解析上传（按 #~### 切题）、手工录入与检索测试；源文件不入库，仅保留文件名与行号。"
  >
    <template #actions>
      <el-button v-permission="'interviewbank:upload'" type="primary" :icon="Upload">上传 / 录入</el-button>
    </template>
  </PageShell>
</template>
