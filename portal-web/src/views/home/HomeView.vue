<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRouter } from 'vue-router'
import StatCounter from '@/components/home/StatCounter.vue'
import OnboardingGuide from '@/components/home/OnboardingGuide.vue'
import {
  getHotStages,
  getLearningOverview,
  getPlatformStats,
  type HotStage,
  type LearningOverview,
  type PlatformStats
} from '@/api/home'
import { finishGuide } from '@/api/profile'
import { useUserStore } from '@/store/user'

interface QuickEntry {
  icon: string
  title: string
  desc: string
  theme: string
  to: RouteLocationRaw
}

interface FeatureItem {
  icon: string
  title: string
  desc: string
  theme: string
  to: RouteLocationRaw
}

const GUIDE_STORAGE_KEY = 'ys_portal_guide_finished'

const router = useRouter()
const userStore = useUserStore()

const platformStats = ref<PlatformStats | null>(null)
const hotStages = ref<HotStage[]>([])
const overview = ref<LearningOverview | null>(null)

const stagesLoading = ref(true)
const overviewLoading = ref(false)
const overviewFailed = ref(false)
const guideVisible = ref(false)

const quickEntries: QuickEntry[] = [
  {
    icon: '🎲',
    title: '随机练习 10 题',
    desc: '随机抽题，快速检验知识储备',
    theme: 'quick-card--random',
    to: { name: 'practice', query: { mode: 'random' } }
  },
  {
    icon: '🤖',
    title: '开始 AI 面试',
    desc: 'AI 面试官模拟真实面试场景',
    theme: 'quick-card--ai',
    to: { name: 'interview-config' }
  },
  {
    icon: '▶️',
    title: '继续上次学习',
    desc: '回到上次练习的位置继续刷题',
    theme: 'quick-card--continue',
    to: { name: 'practice', query: { continue: 'true' } }
  },
  {
    icon: '🗺️',
    title: '规划学习路线',
    desc: '根据方向定制专属学习路径',
    theme: 'quick-card--route',
    to: { name: 'routes' }
  }
]

const features: FeatureItem[] = [
  {
    icon: '📚',
    title: '海量题库',
    desc: '覆盖 AI 应用、大模型、机器人等八大技术方向，持续更新的已上架题目。',
    theme: 'feature-icon--purple',
    to: { name: 'questions' }
  },
  {
    icon: '🤖',
    title: 'AI 模拟面试',
    desc: '真实面试场景逐题作答，AI 面试官实时评分，并生成能力分析报告。',
    theme: 'feature-icon--pink',
    to: { name: 'interview-config' }
  },
  {
    icon: '✏️',
    title: '刷题练习',
    desc: '顺序、随机多种练习模式，客观题即时判分，答错自动沉淀到错题本。',
    theme: 'feature-icon--blue',
    to: { name: 'practice' }
  },
  {
    icon: '🗺️',
    title: '学习路线',
    desc: '按学科方向跟学 B 站精选课程，从入门到就业体系化进阶。',
    theme: 'feature-icon--green',
    to: { name: 'routes' }
  }
]

const stagePalette = [
  { background: 'rgba(102, 126, 234, 0.14)', color: '#5a67d8' },
  { background: 'rgba(240, 147, 251, 0.16)', color: '#c04bb5' },
  { background: 'rgba(79, 172, 254, 0.16)', color: '#2d8fd5' },
  { background: 'rgba(67, 233, 123, 0.16)', color: '#22985a' },
  { background: 'rgba(250, 112, 154, 0.16)', color: '#d3466f' },
  { background: 'rgba(230, 162, 60, 0.16)', color: '#b7791f' }
]

const heroStats = computed(() => [
  {
    label: '精选面试题',
    value: platformStats.value?.interviewQuestionCount ?? null,
    suffix: '',
    decimals: 0
  },
  {
    label: '注册学员',
    value: platformStats.value?.studentCount ?? null,
    suffix: '',
    decimals: 0
  },
  {
    label: 'AI 面试次数',
    value: platformStats.value?.interviewCount ?? null,
    suffix: '',
    decimals: 0
  },
  {
    label: '面试好评率',
    value: normalizeRate(platformStats.value?.goodRate),
    suffix: '%',
    decimals: 1
  }
])

const consecutiveDays = computed(() => overview.value?.learningStats.consecutiveDays ?? null)
const accuracyValue = computed(() => normalizeRate(overview.value?.learningStats.accuracyRate))
const totalPracticeCount = computed(() => overview.value?.learningStats.totalPracticeCount ?? null)
const weekTrend = computed(() => overview.value?.weekTrend ?? [])
const weekMax = computed(() => weekTrend.value.reduce((max, item) => Math.max(max, item.count), 0) || 1)
const hasTrend = computed(() => weekTrend.value.some((item) => item.count > 0))
const todayCount = computed(() => overview.value?.todayCount ?? null)

onMounted(async () => {
  void loadPlatformStats()
  void loadHotStages()
  if (userStore.isLoggedIn) {
    await loadOverview()
  }
  window.setTimeout(maybeShowGuide, 700)
})

async function loadPlatformStats(): Promise<void> {
  try {
    platformStats.value = await getPlatformStats()
  } catch {
    // 接口未就绪时保持占位展示，不阻断首页
    platformStats.value = null
  }
}

async function loadHotStages(): Promise<void> {
  stagesLoading.value = true
  try {
    hotStages.value = await getHotStages()
  } catch {
    hotStages.value = []
  } finally {
    stagesLoading.value = false
  }
}

async function loadOverview(): Promise<void> {
  overviewLoading.value = true
  overviewFailed.value = false
  try {
    overview.value = await getLearningOverview()
  } catch {
    overviewFailed.value = true
  } finally {
    overviewLoading.value = false
  }
}

function maybeShowGuide(): void {
  const finished =
    readGuideFlag() ||
    overview.value?.guideFinished === 1 ||
    userStore.profile?.guideFinished === 1
  if (finished) {
    persistGuideFlag()
    return
  }
  guideVisible.value = true
}

function handleGuideFinish(): void {
  persistGuideFlag()
  if (!userStore.isLoggedIn) {
    return
  }
  finishGuide().catch(() => undefined)
  if (userStore.profile) {
    userStore.profile.guideFinished = 1
  }
}

function readGuideFlag(): boolean {
  try {
    return localStorage.getItem(GUIDE_STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

function persistGuideFlag(): void {
  try {
    localStorage.setItem(GUIDE_STORAGE_KEY, '1')
  } catch {
    // 隐私模式下存储不可用时忽略，仅影响引导展示频率
  }
}

/** 兼容后端返回 0~1 小数或 0~100 百分数两种口径 */
function normalizeRate(value?: number): number | null {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return null
  }
  return value <= 1 ? value * 100 : value
}

function barHeight(count: number): string {
  return `${Math.max((count / weekMax.value) * 100, 6)}%`
}

function formatDay(date: string): string {
  const parts = date.split('-')
  return parts.length >= 2 ? `${parts[parts.length - 2]}-${parts[parts.length - 1]}` : date
}

function stageIconStyle(index: number): Record<string, string> {
  const palette = stagePalette[index % stagePalette.length]
  return { background: palette.background, color: palette.color }
}

function openStage(stage: HotStage): void {
  void router.push({
    name: 'questions',
    query: { subjectId: String(stage.subjectId), stageId: String(stage.stageId) }
  })
}
</script>

<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-glow hero-glow--left" aria-hidden="true" />
      <div class="hero-glow hero-glow--right" aria-hidden="true" />
      <div class="container hero-inner">
        <span class="hero-badge">AI 驱动 · 智能面试训练</span>
        <h1>AI 赋能的智能面试训练平台</h1>
        <p class="hero-subtitle">海量题库 × AI 面试官 × 智能分析，从刷题到面试，助你拿下心仪 Offer</p>

        <div class="hero-actions">
          <el-button class="hero-btn hero-btn--ghost" size="large" round @click="router.push({ name: 'questions' })">
            海量题库
          </el-button>
          <el-button class="hero-btn hero-btn--primary" size="large" round @click="router.push({ name: 'interview-config' })">
            开始 AI 面试
          </el-button>
        </div>

        <div class="hero-stats">
          <div v-for="item in heroStats" :key="item.label" class="hero-stat">
            <strong>
              <StatCounter :value="item.value" :suffix="item.suffix" :decimals="item.decimals" />
            </strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 快速开始 -->
    <section class="quick-band">
      <div class="container">
        <div class="quick-grid">
          <button
            v-for="entry in quickEntries"
            :key="entry.title"
            type="button"
            class="quick-card"
            :class="entry.theme"
            @click="router.push(entry.to)"
          >
            <span class="quick-icon" aria-hidden="true">{{ entry.icon }}</span>
            <span class="quick-info">
              <strong>{{ entry.title }}</strong>
              <small>{{ entry.desc }}</small>
            </span>
            <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </section>

    <!-- 核心功能 -->
    <section class="section section--features" aria-labelledby="feature-title">
      <div class="container">
        <header class="section-head">
          <h2 id="feature-title">核心功能</h2>
          <p>全方位助力你的面试准备</p>
        </header>

        <div class="feature-grid">
          <router-link
            v-for="feature in features"
            :key="feature.title"
            :to="feature.to"
            class="feature-card"
          >
            <span class="feature-icon" :class="feature.theme" aria-hidden="true">{{ feature.icon }}</span>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.desc }}</p>
            <span class="feature-link">进入 <el-icon><ArrowRight /></el-icon></span>
          </router-link>
        </div>
      </div>
    </section>

    <!-- 继续学习 -->
    <section class="section section--learning" aria-labelledby="learning-title">
      <div class="container">
        <header class="section-head">
          <h2 id="learning-title">继续学习</h2>
          <p>坚持每日练习，持续进步</p>
        </header>

        <div v-if="!userStore.isLoggedIn" class="learning-guest">
          <span class="guest-icon" aria-hidden="true">🔒</span>
          <h3>登录后查看学习数据</h3>
          <p>连续学习天数、正确率、总练习数与近 7 天练习趋势，都会在这里汇总。</p>
          <div class="guest-actions">
            <el-button type="primary" round @click="router.push({ name: 'login' })">登录</el-button>
            <el-button round @click="router.push({ name: 'register' })">免费注册</el-button>
          </div>
        </div>

        <div v-else class="learning-grid">
          <article class="learning-panel">
            <header class="panel-head">
              <h3>📊 学习统计</h3>
              <router-link :to="{ name: 'profile' }" class="panel-link">查看全部 →</router-link>
            </header>

            <el-skeleton v-if="overviewLoading" :rows="3" animated />

            <template v-else>
              <div class="stat-badges">
                <div class="stat-badge">
                  <span class="badge-icon" aria-hidden="true">🔥</span>
                  <strong class="badge-value badge-value--streak">
                    <StatCounter :value="consecutiveDays" />
                  </strong>
                  <span class="badge-label">天连续学习</span>
                </div>
                <div class="stat-badge">
                  <span class="badge-icon" aria-hidden="true">🎯</span>
                  <strong class="badge-value badge-value--accuracy">
                    <StatCounter :value="accuracyValue" suffix="%" :decimals="1" />
                  </strong>
                  <span class="badge-label">正确率</span>
                </div>
                <div class="stat-badge">
                  <span class="badge-icon" aria-hidden="true">📝</span>
                  <strong class="badge-value badge-value--total">
                    <StatCounter :value="totalPracticeCount" />
                  </strong>
                  <span class="badge-label">总练习数</span>
                </div>
              </div>
              <p v-if="overviewFailed" class="panel-tip">
                学习数据暂时无法获取
                <el-button link type="primary" @click="loadOverview">重新加载</el-button>
              </p>
            </template>
          </article>

          <article class="learning-panel">
            <header class="panel-head">
              <h3>📈 近 7 天练习</h3>
              <span class="panel-meta">
                今日已刷 <strong>{{ todayCount ?? '--' }}</strong> 题
              </span>
            </header>

            <el-skeleton v-if="overviewLoading" :rows="3" animated />

            <template v-else-if="hasTrend">
              <div class="bar-chart" role="img" aria-label="近 7 天每日练习量">
                <div
                  v-for="(item, index) in weekTrend"
                  :key="item.date"
                  class="bar-item"
                  :class="{ 'bar-item--today': index === weekTrend.length - 1 }"
                >
                  <div class="bar-track">
                    <span class="bar-value">{{ item.count }}</span>
                    <div class="bar" :style="{ height: barHeight(item.count) }" />
                  </div>
                  <span class="bar-label">{{ formatDay(item.date) }}</span>
                </div>
              </div>
            </template>

            <div v-else class="chart-empty">
              <span class="chart-empty-icon" aria-hidden="true">📈</span>
              <p>{{ overviewFailed ? '学习数据暂时无法获取' : '还没有练习记录，完成一次练习后这里会展示趋势' }}</p>
              <el-button v-if="overviewFailed" round size="small" @click="loadOverview">重新加载</el-button>
              <el-button v-else type="primary" round size="small" @click="router.push({ name: 'practice' })">去刷题</el-button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- 热门阶段 -->
    <section class="section section--stages" aria-labelledby="stage-title">
      <div class="container">
        <header class="section-head">
          <h2 id="stage-title">热门阶段</h2>
          <p>选择你的技术方向，开始学习</p>
        </header>

        <div v-if="stagesLoading" class="stage-grid">
          <div v-for="index in 4" :key="index" class="stage-card stage-card--skeleton">
            <el-skeleton :rows="2" animated />
          </div>
        </div>

        <div v-else-if="hotStages.length > 0" class="stage-grid">
          <button
            v-for="(stage, index) in hotStages"
            :key="stage.stageId"
            type="button"
            class="stage-card"
            @click="openStage(stage)"
          >
            <span class="stage-icon" :style="stageIconStyle(index)" aria-hidden="true">
              {{ stage.stageName.slice(0, 1) }}
            </span>
            <span class="stage-info">
              <strong>{{ stage.stageName }}</strong>
              <small>{{ stage.subjectName }}</small>
            </span>
            <span class="stage-count">{{ stage.questionCount }} 题</span>
          </button>
        </div>

        <div v-else class="stages-empty">
          <p>热门阶段正在整理中，先去题库按学科浏览全部题目。</p>
          <div class="stages-empty-actions">
            <el-button round @click="loadHotStages">刷新</el-button>
            <el-button type="primary" round @click="router.push({ name: 'questions' })">进入题库</el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- AI 面试介绍 -->
    <section class="ai-section" aria-labelledby="ai-title">
      <div class="container ai-layout">
        <div class="ai-info">
          <span class="ai-badge">🤖 AI 驱动</span>
          <h2 id="ai-title">AI 模拟面试官</h2>
          <p>
            基于题库与 RAG 知识库，打造接近真实的面试体验。AI 面试官会根据你的回答逐题评分，
            给出即时点评，并在面试结束后生成能力分析与补强建议。
          </p>

          <ul class="ai-features">
            <li><span class="check" aria-hidden="true">✓</span>智能出题，千人千面</li>
            <li><span class="check" aria-hidden="true">✓</span>实时评分，即时反馈</li>
            <li><span class="check" aria-hidden="true">✓</span>面试报告，精准分析</li>
            <li><span class="check" aria-hidden="true">✓</span>海量知识，持续更新</li>
          </ul>

          <el-button class="ai-cta" size="large" round @click="router.push({ name: 'interview-config' })">
            立即体验
          </el-button>
        </div>

        <div class="ai-demo" aria-hidden="true">
          <header class="demo-head">
            <span class="demo-avatar">🤖</span>
            <div class="demo-title">
              <strong>AI 面试官</strong>
              <span><i class="status-dot" />正在面试中…</span>
            </div>
          </header>

          <div class="demo-chat">
            <p class="chat-message chat-message--ai">
              你好，我是你的 AI 面试官。今天我们将进行一场 AI 应用开发方向的面试，请先简单介绍一下你的项目经验。
            </p>
            <p class="chat-message chat-message--user">
              我有两年 AI 应用开发经验，主要做 RAG 检索增强和 Agent 智能体项目。
            </p>
            <p class="chat-message chat-message--ai">
              很好。那请你说说，在 RAG 项目中你是如何提升检索召回质量的？
            </p>
            <div class="typing">
              <span /><span /><span />
              <em>AI 面试官正在分析你的回答…</em>
            </div>
          </div>

          <div class="demo-input">
            <span>输入你的回答…</span>
            <i>➤</i>
          </div>
        </div>
      </div>
    </section>

    <OnboardingGuide v-model="guideVisible" @finish="handleGuideFinish" />
  </div>
</template>

<style scoped>
.home {
  overflow: hidden;
}

/* Hero */
.hero {
  position: relative;
  padding: 86px 0 64px;
  overflow: hidden;
  color: #fff;
  text-align: center;
  background: linear-gradient(135deg, #5a6bdd 0%, #7156c4 52%, #8d5cb9 100%);
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(90px);
}

.hero-glow--left {
  top: -140px;
  left: -100px;
  width: 420px;
  height: 420px;
  background: rgba(255, 255, 255, 0.22);
}

.hero-glow--right {
  right: -140px;
  bottom: -180px;
  width: 480px;
  height: 480px;
  background: rgba(255, 154, 208, 0.22);
}

.hero::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background-image: radial-gradient(rgba(255, 255, 255, 0.16) 1px, transparent 1px);
  background-size: 26px 26px;
  -webkit-mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), transparent 70%);
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), transparent 70%);
}

.hero-inner {
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  font-size: 13px;
  letter-spacing: 0.5px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: var(--ys-radius-round);
  backdrop-filter: blur(4px);
}

.hero h1 {
  margin-top: 20px;
  font-size: clamp(30px, 4.2vw, 46px);
  font-weight: 700;
  line-height: 1.28;
  letter-spacing: 1px;
}

.hero-subtitle {
  max-width: 640px;
  margin: 16px auto 0;
  font-size: 16px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.85);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: center;
  margin-top: 30px;
}

.hero-btn {
  min-width: 150px;
  height: 46px;
  font-size: 15px;
}

.hero-btn--ghost {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.48);
}

.hero-btn--ghost:hover {
  color: #5a6bdd;
  background: #fff;
  border-color: #fff;
}

.hero-btn--primary {
  color: #5a6bdd;
  background: #fff;
  border: none;
  box-shadow: 0 8px 24px rgba(38, 31, 84, 0.24);
}

.hero-btn--primary:hover {
  color: #5a6bdd;
  background: #fff;
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(38, 31, 84, 0.3);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 48px;
  padding-top: 28px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  text-align: center;
}

.hero-stat + .hero-stat {
  border-left: 1px solid rgba(255, 255, 255, 0.14);
}

.hero-stat strong {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
}

.hero-stat span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
}

/* 快速开始 */
.quick-band {
  padding: 30px 0;
  background: #fff;
  border-bottom: 1px solid #eef0f6;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.quick-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 18px;
  overflow: hidden;
  color: #fff;
  text-align: left;
  cursor: pointer;
  border: none;
  border-radius: 16px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.quick-card::after {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  content: '';
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), transparent);
  transition: left 0.5s ease;
}

.quick-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(38, 44, 92, 0.2);
}

.quick-card:hover::after {
  left: 100%;
}

.quick-card--random {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.quick-card--ai {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.quick-card--continue {
  background: linear-gradient(135deg, #4facfe 0%, #00c6fb 100%);
}

.quick-card--route {
  background: linear-gradient(135deg, #43e97b 0%, #2bc7ad 100%);
}

.quick-icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  font-size: 22px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 13px;
}

.quick-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.quick-info strong {
  font-size: 15px;
  font-weight: 600;
}

.quick-info small {
  display: -webkit-box;
  margin-top: 3px;
  overflow: hidden;
  font-size: 11.5px;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.82);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.quick-arrow {
  flex: none;
  font-size: 16px;
  opacity: 0;
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.quick-card:hover .quick-arrow {
  opacity: 1;
  transform: translateX(3px);
}

/* 通用区块 */
.section {
  padding: 72px 0;
}

.section--features {
  background: #f7f8fc;
}

.section--learning {
  background: #fff;
}

.section--stages {
  background: #f7f6fc;
}

.section-head {
  max-width: 640px;
  margin: 0 auto 38px;
  text-align: center;
}

.section-head h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.section-head p {
  margin-top: 8px;
  font-size: 14px;
  color: var(--ys-text-secondary);
}

/* 核心功能 */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.feature-card {
  display: flex;
  flex-direction: column;
  padding: 28px 24px;
  color: var(--ys-text-primary);
  background: #fff;
  border: 1px solid #eef0f6;
  border-radius: 16px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.feature-card:hover {
  color: var(--ys-text-primary);
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(46, 56, 110, 0.1);
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 18px;
  font-size: 26px;
  border-radius: 16px;
}

.feature-icon--purple {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.18), rgba(118, 75, 162, 0.12));
}

.feature-icon--pink {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.2), rgba(245, 87, 108, 0.12));
}

.feature-icon--blue {
  background: linear-gradient(135deg, rgba(79, 172, 254, 0.18), rgba(0, 198, 251, 0.12));
}

.feature-icon--green {
  background: linear-gradient(135deg, rgba(67, 233, 123, 0.18), rgba(56, 249, 215, 0.12));
}

.feature-card h3 {
  font-size: 17px;
  font-weight: 600;
}

.feature-card p {
  flex: 1;
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ys-text-secondary);
}

.feature-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ys-primary);
}

/* 学习统计 */
.learning-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.learning-panel {
  padding: 26px;
  background: #fff;
  border: 1px solid #eef0f6;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(46, 56, 110, 0.04);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 22px;
}

.panel-head h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.panel-link {
  font-size: 13px;
  color: var(--ys-primary);
}

.panel-link:hover {
  text-decoration: underline;
}

.panel-meta {
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.panel-meta strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--ys-primary);
}

.stat-badges {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.stat-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 10px;
  text-align: center;
  background: var(--ys-bg);
  border-radius: 12px;
}

.badge-icon {
  margin-bottom: 6px;
  font-size: 26px;
  line-height: 1;
}

.badge-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.badge-value--streak {
  color: #f5576c;
}

.badge-value--accuracy {
  color: var(--ys-success);
}

.badge-value--total {
  color: #4f6ef7;
}

.badge-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.panel-tip {
  margin-top: 14px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

/* 柱状图 */
.bar-chart {
  display: flex;
  align-items: stretch;
  gap: 12px;
  height: 168px;
  padding: 0 4px;
}

.bar-item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.bar-value {
  margin-bottom: 5px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ys-text-secondary);
}

.bar-track {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  border-bottom: 1px solid var(--ys-border-light);
}

.bar {
  width: 58%;
  max-width: 34px;
  min-height: 6px;
  background: linear-gradient(180deg, #8b9cf0 0%, #667eea 100%);
  border-radius: 6px 6px 0 0;
  transition: height 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.bar-label {
  overflow: hidden;
  font-size: 11px;
  color: var(--ys-text-secondary);
  white-space: nowrap;
}

.bar-item--today .bar {
  background: linear-gradient(180deg, #f6a6d8 0%, #f5576c 100%);
}

.bar-item--today .bar-value {
  color: #f5576c;
}

.bar-item--today .bar-label {
  font-weight: 600;
  color: #f5576c;
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 168px;
  padding: 12px;
  text-align: center;
}

.chart-empty-icon {
  margin-bottom: 8px;
  font-size: 30px;
  opacity: 0.8;
}

.chart-empty p {
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.learning-guest {
  max-width: 560px;
  padding: 44px 24px;
  margin: 0 auto;
  text-align: center;
  background: #fff;
  border: 1px dashed #dfe3f0;
  border-radius: 16px;
}

.guest-icon {
  font-size: 34px;
}

.learning-guest h3 {
  margin-top: 10px;
  font-size: 18px;
  font-weight: 600;
}

.learning-guest p {
  margin: 8px 0 20px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ys-text-secondary);
}

.guest-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 热门阶段 */
.stage-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stage-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #eef0f6;
  border-radius: 14px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.stage-card:hover {
  border-color: #dfe4f8;
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(46, 56, 110, 0.08);
}

.stage-card--skeleton {
  cursor: default;
  pointer-events: none;
}

.stage-card--skeleton:hover {
  transform: none;
  box-shadow: none;
}

.stage-icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  font-size: 18px;
  font-weight: 700;
  border-radius: 13px;
}

.stage-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.stage-info strong {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  color: var(--ys-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-info small {
  margin-top: 2px;
  overflow: hidden;
  font-size: 12px;
  color: var(--ys-text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-count {
  flex: none;
  padding: 3px 8px;
  font-size: 11px;
  color: var(--ys-primary);
  white-space: nowrap;
  background: rgba(64, 158, 255, 0.1);
  border-radius: var(--ys-radius-round);
}

.stages-empty {
  padding: 40px 20px;
  text-align: center;
  background: #fff;
  border: 1px dashed #dfe3f0;
  border-radius: 16px;
}

.stages-empty p {
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.stages-empty-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* AI 面试介绍 */
.ai-section {
  position: relative;
  padding: 78px 0;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #171a2b 0%, #1f2340 55%, #2a2145 100%);
}

.ai-section::before {
  position: absolute;
  top: -120px;
  right: -80px;
  width: 400px;
  height: 400px;
  pointer-events: none;
  content: '';
  background: rgba(118, 75, 162, 0.35);
  border-radius: 50%;
  filter: blur(100px);
}

.ai-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 56px;
  align-items: center;
}

.ai-badge {
  display: inline-block;
  padding: 5px 14px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--ys-radius-round);
}

.ai-info h2 {
  margin-top: 18px;
  font-size: 34px;
  font-weight: 700;
}

.ai-info > p {
  margin-top: 14px;
  font-size: 14px;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.72);
}

.ai-features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px 20px;
  margin: 24px 0 30px;
}

.ai-features li {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.86);
}

.check {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 11px;
  color: #7cf29c;
  background: rgba(103, 194, 58, 0.16);
  border-radius: 50%;
}

.ai-cta {
  padding: 0 34px;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #8d5cb9 100%);
  border: none;
  box-shadow: 0 10px 26px rgba(102, 126, 234, 0.35);
}

.ai-cta:hover {
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(102, 126, 234, 0.45);
}

.ai-demo {
  padding: 26px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(6px);
}

.demo-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.demo-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  font-size: 22px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
}

.demo-title {
  display: flex;
  flex-direction: column;
}

.demo-title strong {
  font-size: 15px;
  font-weight: 600;
}

.demo-title span {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #7cf29c;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(124, 242, 156, 0.16);
}

.demo-chat {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 0;
}

.chat-message {
  max-width: 86%;
  padding: 11px 14px;
  font-size: 13px;
  line-height: 1.75;
  border-radius: 14px;
}

.chat-message--ai {
  background: rgba(124, 242, 156, 0.12);
  border-bottom-left-radius: 4px;
}

.chat-message--user {
  align-self: flex-end;
  background: rgba(102, 126, 234, 0.32);
  border-bottom-right-radius: 4px;
}

.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.typing span {
  width: 5px;
  height: 5px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  animation: typing 1.2s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

.typing em {
  margin-left: 6px;
  font-style: normal;
}

@keyframes typing {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.demo-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--ys-radius-round);
}

.demo-input i {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  font-size: 13px;
  font-style: normal;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
}

/* 响应式 */
@media (max-width: 1080px) {
  .quick-grid,
  .feature-grid,
  .stage-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .learning-grid,
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .ai-layout {
    gap: 40px;
  }

  .ai-info {
    max-width: 640px;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 60px 0 48px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .hero-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px 0;
  }

  .hero-stat:nth-child(3) {
    border-left: none;
  }

  .hero-stat strong {
    font-size: 28px;
  }

  .section {
    padding: 48px 0;
  }

  .section-head h2 {
    font-size: 24px;
  }

  .section-head {
    margin-bottom: 28px;
  }

  .ai-section {
    padding: 52px 0;
  }

  .ai-info h2 {
    font-size: 28px;
  }

  .ai-demo {
    padding: 20px;
  }

  .learning-panel {
    padding: 20px;
  }

  .stat-badges {
    gap: 8px;
  }

  .stat-badge {
    padding: 16px 6px;
  }

  .badge-value {
    font-size: 22px;
  }

  .bar-chart {
    height: 140px;
    gap: 6px;
  }

  .bar-label {
    font-size: 10px;
  }
}

@media (max-width: 560px) {
  .quick-grid,
  .feature-grid,
  .stage-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-btn {
    width: 100%;
    margin: 0;
  }

  .ai-features {
    grid-template-columns: 1fr;
  }

  .chat-message {
    max-width: 92%;
  }

  .guest-actions {
    flex-direction: column;
  }

  .guest-actions .el-button {
    margin: 0;
  }
}
</style>
