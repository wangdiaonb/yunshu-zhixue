<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const profile = computed(() => userStore.profile)

onMounted(() => {
  if (!userStore.profile) {
    userStore.fetchProfile().catch(() => undefined)
  }
})

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  await router.replace('/')
}

/** 兼容后端返回 0~1 小数或 0~100 百分数两种口径 */
function formatRate(value?: number): string {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--'
  }
  const percent = value <= 1 ? value * 100 : value
  return `${percent.toFixed(1)}%`
}
</script>

<template>
  <PageShell title="个人中心" description="维护个人资料，查看练习、面试与错题累计数据。">
    <el-skeleton v-if="!profile" :rows="6" animated />

    <template v-else>
      <div class="profile-head">
        <el-avatar :size="64" :src="profile.avatar || undefined">
          {{ profile.nickname.slice(0, 1) }}
        </el-avatar>
        <div class="profile-meta">
          <h2>{{ profile.nickname }}</h2>
          <span class="text-muted">{{ profile.mobile }}</span>
        </div>
        <el-button class="head-action" @click="handleLogout">退出登录</el-button>
      </div>

      <el-descriptions :column="2" border class="profile-desc">
        <el-descriptions-item label="昵称">{{ profile.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ profile.mobile }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ profile.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ ['未知', '男', '女'][profile.gender] ?? '未知' }}
        </el-descriptions-item>
        <el-descriptions-item label="练习次数">{{ profile.stats.practiceCount }}</el-descriptions-item>
        <el-descriptions-item label="面试次数">{{ profile.stats.interviewCount }}</el-descriptions-item>
        <el-descriptions-item label="错题数">{{ profile.stats.wrongQuestionCount }}</el-descriptions-item>
        <el-descriptions-item label="累计学习天数">{{ profile.study.totalDays }}</el-descriptions-item>
        <el-descriptions-item label="正确率">
          {{ formatRate(profile.study.accuracyRate) }}
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </PageShell>
</template>

<style scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--ys-border-light);
  margin-bottom: 20px;
}

.profile-meta h2 {
  font-size: 20px;
  font-weight: 600;
}

.profile-meta span {
  font-size: 13px;
}

.head-action {
  margin-left: auto;
}

@media (max-width: 600px) {
  .profile-desc {
    --el-descriptions-table-border: none;
  }
}
</style>
