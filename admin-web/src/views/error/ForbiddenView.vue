<script setup lang="ts">
import { HomeFilled } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
</script>

<template>
  <PageShell title="无权访问" description="当前账号没有访问该页面的权限，请联系超级管理员分配权限。">
    <el-result icon="warning" title="403" sub-title="权限不足，无法查看该页面内容">
      <template #extra>
        <el-button type="primary" :icon="HomeFilled" @click="$router.replace('/dashboard')">返回数据看板</el-button>
      </template>
    </el-result>
  </PageShell>
</template>
