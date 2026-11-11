<template>
  <div class="not-found">
    <span class="brand-mark mark" aria-hidden="true" />
    <h1>404</h1>
    <p>你访问的页面不存在或已被移除。</p>
    <el-button type="primary" round @click="$router.replace('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--ys-bg);
  color: var(--ys-text-primary);
}

.mark {
  font-size: 48px;
}

.not-found h1 {
  font-size: 56px;
  font-weight: 700;
  background: var(--ys-brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.not-found p {
  color: var(--ys-text-secondary);
  margin-bottom: 8px;
}
</style>
