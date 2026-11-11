<script setup lang="ts">
withDefaults(
  defineProps<{
    icon?: string
    title?: string
    description?: string
  }>(),
  {
    icon: '🛠️',
    title: '页面待建设',
    description: '页面基本布局已就绪，业务内容将在后续迭代中接入。'
  }
)
</script>

<template>
  <div class="page-placeholder">
    <div class="page-placeholder__icon">{{ icon }}</div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<style scoped>
.page-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 300px;
  padding: 40px 24px;
  text-align: center;
}

.page-placeholder__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 8px;
  border-radius: 18px;
  font-size: 30px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.12) 100%);
}

.page-placeholder h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.page-placeholder p {
  max-width: 460px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ys-text-secondary);
}
</style>
