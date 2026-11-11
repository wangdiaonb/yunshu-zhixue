<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PagePlaceholder from '@/components/PagePlaceholder.vue'

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    /** 内容区是否使用白色卡片容器；统计类页面可设为 false */
    card?: boolean
    /** 卡片内容区最小高度（仅在 card=true 时生效） */
    bodyMinHeight?: string
  }>(),
  {
    description: '',
    card: true,
    bodyMinHeight: '360px'
  }
)

const route = useRoute()
const emptyIcon = computed(() => (route.meta.icon as string | undefined) ?? '🛠️')
</script>

<template>
  <div class="page-shell">
    <header class="page-shell__header">
      <div class="page-shell__heading">
        <h1>{{ props.title }}</h1>
        <p v-if="props.description">{{ props.description }}</p>
      </div>
      <div v-if="$slots.actions" class="page-shell__actions">
        <slot name="actions" />
      </div>
    </header>

    <div v-if="$slots.toolbar" class="page-shell__toolbar">
      <slot name="toolbar" />
    </div>

    <section
      class="page-shell__body"
      :class="{ 'is-plain': !props.card }"
      :style="props.card ? { minHeight: props.bodyMinHeight } : undefined"
    >
      <slot>
        <PagePlaceholder :icon="emptyIcon" />
      </slot>
    </section>
  </div>
</template>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.page-shell__heading {
  min-width: 0;
}

.page-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.page-shell__heading h1 {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--ys-text-primary);
}

.page-shell__heading p {
  margin-top: 4px;
  max-width: 860px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ys-text-secondary);
  overflow-wrap: anywhere;
}

.page-shell__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.page-shell__toolbar {
  padding: 16px 20px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.page-shell__body {
  padding: 24px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.page-shell__body.is-plain {
  padding: 0;
  background: transparent;
  border-radius: 0;
  box-shadow: none;
}

@media (max-width: 768px) {
  .page-shell__heading h1 {
    font-size: 20px;
  }

  .page-shell__body {
    padding: 16px;
  }
}
</style>
