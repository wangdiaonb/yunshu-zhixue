<script setup lang="ts">
import { computed } from 'vue'
import { useSiteStore } from '@/store/site'

const siteStore = useSiteStore()

const props = defineProps<{ modelValue: boolean }>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'finish'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const steps = [
  {
    icon: '📂',
    title: '选择技术方向',
    desc: '在热门阶段中选择要准备的技术栈，Java、前端、AI 应用等任你挑选。'
  },
  {
    icon: '✏️',
    title: '开始刷题练习',
    desc: '顺序练习、随机练习多种模式，即时判分并自动沉淀错题。'
  },
  {
    icon: '🤖',
    title: 'AI 面试检验',
    desc: 'AI 面试官模拟真实面试，智能评分并生成能力报告与补强建议。'
  }
]

function close(): void {
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    width="540px"
    align-center
    :show-close="false"
    :close-on-click-modal="false"
    class="guide-dialog"
    @closed="emit('finish')"
  >
    <div class="guide">
      <div class="guide-icon" aria-hidden="true">🚀</div>
      <h2>欢迎来到{{ siteStore.siteName }}</h2>
      <p class="guide-subtitle">三步开启你的面试备战之旅</p>

      <ol class="guide-steps">
        <li v-for="(step, index) in steps" :key="step.title" class="guide-step">
          <span class="step-index">{{ index + 1 }}</span>
          <span class="step-icon" aria-hidden="true">{{ step.icon }}</span>
          <div class="step-text">
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </li>
      </ol>

      <div class="guide-actions">
        <el-button type="primary" size="large" round @click="close">开始学习</el-button>
        <el-button text @click="close">跳过引导</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.guide {
  padding: 8px 8px 4px;
  text-align: center;
}

.guide-icon {
  font-size: 52px;
  line-height: 1;
}

.guide h2 {
  margin-top: 14px;
  font-size: 24px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.guide-subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: var(--ys-text-secondary);
}

.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 26px 0 28px;
  text-align: left;
}

.guide-step {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  background: var(--ys-bg);
  border-radius: var(--ys-radius-lg);
  transition: background 0.2s ease;
}

.guide-step:hover {
  background: #eef1fb;
}

.step-index {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  margin-top: 2px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--ys-brand-gradient);
  border-radius: 50%;
}

.step-icon {
  flex: none;
  font-size: 24px;
  line-height: 1.3;
}

.step-text h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.step-text p {
  margin-top: 2px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ys-text-secondary);
}

.guide-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.guide-actions .el-button {
  margin: 0;
  padding-right: 42px;
  padding-left: 42px;
}

@media (max-width: 600px) {
  .guide h2 {
    font-size: 20px;
  }

  .guide-step {
    padding: 12px;
  }
}
</style>
