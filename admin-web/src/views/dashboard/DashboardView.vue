<script setup lang="ts">
import PageShell from '@/components/PageShell.vue'

const statCards = [
  { key: 'subjects', label: '学科数', icon: '🎓', color: 'blue' },
  { key: 'questions', label: '题目数', icon: '📚', color: 'green' },
  { key: 'students', label: '注册用户', icon: '👥', color: 'orange' },
  { key: 'interviews', label: 'AI 面试次数', icon: '🎤', color: 'purple' }
]
</script>

<template>
  <PageShell
    title="数据看板"
    description="学科、题目、学员与面试的运营概览，指标缓存 5 分钟。"
    :card="false"
  >
    <div class="dashboard-body">
      <section class="stats-grid">
        <article v-for="card in statCards" :key="card.key" class="stat-card">
          <span class="stat-icon" :class="card.color">{{ card.icon }}</span>
          <div class="stat-info">
            <strong>--</strong>
            <span>{{ card.label }}</span>
          </div>
        </article>
      </section>

      <section class="panels">
        <article class="panel">
          <header class="panel-header">
            <h2>用户增长趋势</h2>
            <span class="text-muted">近 7 / 30 / 90 天</span>
          </header>
          <el-empty description="接入 A-D02 用户增长接口后展示折线图" />
        </article>

        <article class="panel">
          <header class="panel-header">
            <h2>最近活动</h2>
            <span class="text-muted">来源：操作日志</span>
          </header>
          <el-empty description="接入 A-D04 最近活动接口后展示" />
        </article>
      </section>
    </div>
  </PageShell>
</template>

<style scoped>
.dashboard-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  font-size: 26px;
}

.stat-icon.blue {
  background: rgba(64, 158, 255, 0.1);
}

.stat-icon.green {
  background: rgba(103, 194, 58, 0.1);
}

.stat-icon.orange {
  background: rgba(230, 162, 60, 0.1);
}

.stat-icon.purple {
  background: rgba(114, 69, 209, 0.1);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-info strong {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-info span {
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.panels {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
}

.panel {
  min-height: 320px;
  padding: 22px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-header h2 {
  font-size: 16px;
  font-weight: 600;
}

.panel-header span {
  font-size: 12px;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .panels {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
