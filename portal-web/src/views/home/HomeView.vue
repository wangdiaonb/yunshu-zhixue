<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getPlatformStats, type PlatformStats } from '@/api/home'

const stats = ref<PlatformStats | null>(null)

const statCards = computed(() => [
  { label: '精选面试题', value: format(stats.value?.interviewQuestionCount) },
  { label: '注册学员', value: format(stats.value?.studentCount) },
  { label: 'AI 面试次数', value: format(stats.value?.interviewCount) },
  { label: '面试好评率', value: formatRate(stats.value?.goodRate) }
])

const entries = [
  { icon: '📚', title: '题库中心', desc: '按学科与阶段浏览已上架题目', to: '/questions' },
  { icon: '📝', title: '刷题练习', desc: '顺序或随机练习，客观题即时判分', to: '/practice' },
  { icon: '🗺️', title: '学习路线', desc: '按路线逐课学习并勾选完成', to: '/routes' },
  { icon: '🎤', title: 'AI 模拟面试', desc: '逐题作答、实时评分与能力报告', to: '/interview' }
]

onMounted(async () => {
  try {
    stats.value = await getPlatformStats()
  } catch {
    // 后端接口接入前保持占位展示，不阻断首页
  }
})

function format(value?: number): string {
  return typeof value === 'number' ? value.toLocaleString('zh-CN') : '--'
}

function formatRate(value?: number): string {
  if (typeof value !== 'number') {
    return '--'
  }
  const percent = value <= 1 ? value * 100 : value
  return `${percent.toFixed(1)}%`
}
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-content">
          <span class="hero-badge">AI 赋能 · 智能面试训练</span>
          <h1>让每一次练习<br />都更接近真实的技术面试</h1>
          <p>
            覆盖八大 AI 学科方向的题库与学习路线，结合大模型逐题评分与能力报告，
            帮助你把知识练成可以讲清楚的答案。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="$router.push('/practice')">
              开始刷题
            </el-button>
            <el-button size="large" round class="hero-ghost" @click="$router.push('/interview')">
              进入 AI 面试
            </el-button>
          </div>
        </div>
        <aside class="hero-panel" aria-hidden="true">
          <div class="panel-row">
            <span class="panel-dot" />
            <span>正在分析作答要点…</span>
          </div>
          <div class="panel-line w-90" />
          <div class="panel-line w-75" />
          <div class="panel-line w-60" />
          <div class="panel-score">
            <strong>8.5</strong>
            <span>综合评分</span>
          </div>
        </aside>
      </div>
    </section>

    <section class="container stats">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <strong>{{ card.value }}</strong>
        <span>{{ card.label }}</span>
      </div>
    </section>

    <section class="container entries">
      <header class="entries-header">
        <h2>从这里开始</h2>
        <p>按你的目标选择练习方式，学习数据会自动沉淀到个人中心。</p>
      </header>
      <div class="entry-grid">
        <router-link v-for="entry in entries" :key="entry.to" :to="entry.to" class="entry-card">
          <span class="entry-icon">{{ entry.icon }}</span>
          <h3>{{ entry.title }}</h3>
          <p>{{ entry.desc }}</p>
          <span class="entry-arrow">进入 →</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  background: var(--ys-brand-gradient);
  color: #fff;
  padding: 72px 0 96px;
}

.hero-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
}

.hero-content {
  max-width: 560px;
}

.hero-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: var(--ys-radius-round);
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  letter-spacing: 0.5px;
}

.hero-content h1 {
  margin-top: 20px;
  font-size: 42px;
  line-height: 1.3;
  font-weight: 700;
}

.hero-content p {
  margin-top: 16px;
  font-size: 15px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.86);
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-ghost {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.4);
  color: #fff;
}

.hero-panel {
  width: 340px;
  flex: none;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: var(--ys-radius-lg);
  padding: 24px;
  backdrop-filter: blur(8px);
}

.panel-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 18px;
}

.panel-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7cf29c;
  box-shadow: 0 0 0 4px rgba(124, 242, 156, 0.2);
}

.panel-line {
  height: 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.28);
  margin-bottom: 10px;
}

.w-90 { width: 90%; }
.w-75 { width: 75%; }
.w-60 { width: 60%; }

.panel-score {
  margin-top: 22px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.panel-score strong {
  font-size: 40px;
  font-weight: 700;
}

.panel-score span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: -36px;
}

.stat-card {
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-card strong {
  font-size: 30px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.stat-card span {
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.entries {
  padding-top: 56px;
  padding-bottom: 64px;
}

.entries-header h2 {
  font-size: 26px;
  font-weight: 700;
}

.entries-header p {
  margin-top: 6px;
  color: var(--ys-text-secondary);
}

.entry-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.entry-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
  padding: 24px;
  color: var(--ys-text-primary);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.entry-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--ys-shadow-md);
}

.entry-icon {
  font-size: 28px;
}

.entry-card h3 {
  font-size: 17px;
  font-weight: 600;
}

.entry-card p {
  font-size: 13px;
  color: var(--ys-text-secondary);
  line-height: 1.7;
}

.entry-arrow {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ys-primary);
}

@media (max-width: 960px) {
  .hero-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-panel {
    width: 100%;
  }

  .stats,
  .entry-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .hero {
    padding: 48px 0 80px;
  }

  .hero-content h1 {
    font-size: 30px;
  }

  .stats,
  .entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
