<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useSiteStore } from '@/store/site'

const siteStore = useSiteStore()

withDefaults(
  defineProps<{
    title: string
    description: string
    icon?: string
    features?: string[]
  }>(),
  {
    icon: '🛠️',
    features: () => []
  }
)

const router = useRouter()
</script>

<template>
  <div class="placeholder-page">
    <header class="placeholder-head">
      <div class="container">
        <span class="head-eyebrow">{{ siteStore.siteName }} · 学员端</span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </header>

    <div class="container placeholder-body">
      <section class="placeholder-card">
        <span class="placeholder-icon" aria-hidden="true">{{ icon }}</span>
        <h2>页面建设中</h2>
        <p class="placeholder-hint">该模块正在按原型与需求逐步接入，敬请期待。</p>

        <ul v-if="features.length > 0" class="feature-list">
          <li v-for="feature in features" :key="feature">{{ feature }}</li>
        </ul>

        <div class="placeholder-actions">
          <el-button type="primary" round @click="router.push({ name: 'home' })">返回首页</el-button>
          <el-button round @click="router.back()">返回上一页</el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  min-height: calc(100vh - 64px);
  padding-bottom: 64px;
}

.placeholder-head {
  padding: 44px 0 40px;
  background:
    radial-gradient(600px 220px at 12% 0%, rgba(102, 126, 234, 0.16), transparent 70%),
    linear-gradient(135deg, #f4f6ff 0%, #f8f5fd 100%);
  border-bottom: 1px solid #eef0f6;
}

.head-eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1px;
  color: #7c89d9;
}

.placeholder-head h1 {
  font-size: 30px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.placeholder-head p {
  max-width: 640px;
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ys-text-secondary);
}

.placeholder-body {
  margin-top: 32px;
}

.placeholder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 24px;
  text-align: center;
  background: #fff;
  border: 1px dashed #dfe3f0;
  border-radius: 18px;
}

.placeholder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  font-size: 32px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.14), rgba(118, 75, 162, 0.1));
  border-radius: 20px;
}

.placeholder-card h2 {
  margin-top: 18px;
  font-size: 20px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.placeholder-hint {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 620px;
  margin-top: 22px;
}

.feature-list li {
  padding: 6px 14px;
  font-size: 12px;
  color: #5f6cbf;
  background: #f2f4ff;
  border-radius: var(--ys-radius-round);
}

.placeholder-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

@media (max-width: 600px) {
  .placeholder-head {
    padding: 32px 0 28px;
  }

  .placeholder-head h1 {
    font-size: 24px;
  }

  .placeholder-card {
    padding: 40px 16px;
  }

  .placeholder-actions {
    flex-direction: column;
  }

  .placeholder-actions .el-button {
    margin: 0;
  }
}
</style>
