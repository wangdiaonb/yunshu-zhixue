<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    emptyText?: string
  }>(),
  {
    description: '',
    emptyText: '页面骨架已就绪，待接口与业务逻辑接入'
  }
)
</script>

<template>
  <div class="container page-shell">
    <header class="page-shell__header">
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </header>
    <section class="page-shell__body">
      <slot>
        <el-empty :description="emptyText" />
      </slot>
    </section>
  </div>
</template>

<style scoped>
.page-shell {
  padding-top: 32px;
  padding-bottom: 48px;
}

.page-shell__header {
  margin-bottom: 20px;
}

.page-shell__header h1 {
  font-size: 26px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.page-shell__header p {
  margin-top: 6px;
  color: var(--ys-text-secondary);
}

.page-shell__body {
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
  padding: 28px;
  min-height: 320px;
}

@media (max-width: 768px) {
  .page-shell__body {
    padding: 16px;
  }
}
</style>
