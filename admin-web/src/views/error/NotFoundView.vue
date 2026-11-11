<script setup lang="ts">
import { HomeFilled } from '@element-plus/icons-vue'
</script>

<template>
  <div class="error-page">
    <span class="brand-mark mark" aria-hidden="true" />
    <h1>404</h1>
    <p>页面不存在或已被移除。</p>
    <el-button type="primary" round :icon="HomeFilled" @click="$router.replace('/dashboard')">返回数据看板</el-button>
  </div>
</template>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--ys-bg);
}

.mark {
  font-size: 48px;
}

.error-page h1 {
  font-size: 56px;
  font-weight: 700;
  background: var(--ys-brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.error-page p {
  color: var(--ys-text-secondary);
  margin-bottom: 8px;
}
</style>
