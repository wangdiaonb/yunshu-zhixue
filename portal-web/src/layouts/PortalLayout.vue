<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { name: 'home', label: '首页' },
  { name: 'questions', label: '题库中心' },
  { name: 'practice', label: '刷题练习' },
  { name: 'wrong-book', label: '错题本' },
  { name: 'routes', label: '学习路线' },
  { name: 'interview-config', label: 'AI 面试' }
]

const activeName = computed(() => route.name)

async function handleCommand(command: string): Promise<void> {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出登录')
    await router.push({ name: 'home' })
    return
  }
  await router.push({ name: command })
}
</script>

<template>
  <div class="portal-layout">
    <header class="navbar">
      <div class="container nav-container">
        <router-link to="/" class="nav-logo">
          <span class="brand-mark logo-icon" aria-hidden="true" />
          <span>云枢智学</span>
        </router-link>

        <nav class="nav-menu">
          <router-link
            v-for="item in navItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="nav-link"
            :class="{ active: activeName === item.name }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="nav-actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="nav-user">
                <el-avatar :size="36" :src="userStore.profile?.avatar || undefined">
                  {{ userStore.displayName.slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="interview-records">面试记录</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push({ name: 'login' })">登录</el-button>
            <el-button type="primary" round @click="router.push({ name: 'register' })">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>

    <footer class="footer">
      <div class="container footer-inner">
        <span class="footer-brand">
          <span class="brand-mark" aria-hidden="true" />
          云枢智学
        </span>
        <span class="text-muted">AI 赋能的智能面试训练平台</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.portal-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  gap: 16px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--ys-text-primary);
  white-space: nowrap;
}

.logo-icon {
  font-size: 28px;
}

.nav-menu {
  display: flex;
  gap: 4px;
  overflow-x: auto;
}

.nav-link {
  padding: 8px 14px;
  color: var(--ys-text-regular);
  font-weight: 500;
  border-radius: var(--ys-radius-md);
  white-space: nowrap;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: var(--ys-primary);
  background: rgba(64, 158, 255, 0.05);
}

.nav-link.active {
  color: var(--ys-primary);
  background: rgba(64, 158, 255, 0.1);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: var(--ys-radius-round);
  cursor: pointer;
  transition: background 0.2s ease;
}

.nav-user:hover {
  background: var(--ys-bg);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ys-text-primary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-content {
  flex: 1;
}

.footer {
  border-top: 1px solid var(--ys-border-light);
  background: #fff;
  padding: 20px 0;
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

@media (max-width: 960px) {
  .nav-menu {
    display: none;
  }
}
</style>
