<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const currentYear = new Date().getFullYear()
const logoBroken = ref(false)
const showLogo = computed(() => Boolean(siteStore.logoUrl) && !logoBroken.value)

watch(
  () => siteStore.logoUrl,
  () => {
    logoBroken.value = false
  }
)

const navItems = [
  { name: 'home', label: '首页' },
  { name: 'routes', label: '学习路线' },
  { name: 'questions', label: '题库' },
  { name: 'interview-config', label: 'AI 面试' }
]

const productLinks = [
  { name: 'routes', label: '学习路线' },
  { name: 'questions', label: '题库' },
  { name: 'interview-config', label: 'AI 面试' }
]

const supportLinks = ['帮助中心', '意见反馈', '联系我们']
const aboutLinks = ['关于我们', '用户协议', '隐私政策']

const adminUrl = import.meta.env.VITE_ADMIN_URL || '/admin/'
const mobileMenuOpen = ref(false)

/** 练习、错题等二级页面不属于主导航，保持无高亮 */
const activeNavName = computed(() => {
  const path = route.path
  if (path === '/') {
    return 'home'
  }
  if (path.startsWith('/routes')) {
    return 'routes'
  }
  if (path.startsWith('/questions')) {
    return 'questions'
  }
  if (path.startsWith('/interview')) {
    return 'interview-config'
  }
  return ''
})

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
  }
)

async function handleCommand(command: string): Promise<void> {
  mobileMenuOpen.value = false
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

function openBuilding(name: string): void {
  ElMessage.info(`${name}功能建设中，敬请期待`)
}
</script>

<template>
  <div class="portal-layout">
    <header class="navbar">
      <div class="container nav-container">
        <router-link to="/" class="nav-logo" :aria-label="`${siteStore.siteName}首页`">
          <img
            v-if="showLogo"
            :src="siteStore.logoUrl"
            :alt="siteStore.siteName"
            class="logo-image"
            @error="logoBroken = true"
          />
          <span v-else class="brand-mark logo-icon" aria-hidden="true" />
          <span class="logo-text">{{ siteStore.siteName }}</span>
        </router-link>

        <nav class="nav-menu" aria-label="主导航">
          <router-link
            v-for="item in navItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="nav-link"
            :class="{ active: activeNavName === item.name }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="nav-actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="nav-user">
                <el-avatar
                  :size="34"
                  :src="userStore.profile?.avatar || siteStore.defaultAvatar || undefined"
                  class="nav-avatar"
                >
                  {{ userStore.displayName.slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.displayName }}</span>
                <el-icon class="user-caret"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="wrong-book">我的错题</el-dropdown-item>
                  <el-dropdown-item command="interview-records">面试记录</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <div class="guest-actions">
              <el-button text @click="router.push({ name: 'login' })">登录</el-button>
              <el-button type="primary" round @click="router.push({ name: 'register' })">注册</el-button>
            </div>
          </template>

          <button
            class="menu-toggle"
            type="button"
            aria-label="打开导航菜单"
            @click="mobileMenuOpen = true"
          >
            <el-icon :size="20"><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>

    <footer class="footer">
      <div class="container footer-main">
        <div class="footer-brand">
          <router-link to="/" class="footer-logo">
            <img
              v-if="showLogo"
              :src="siteStore.logoUrl"
              :alt="siteStore.siteName"
              class="footer-logo-image"
              @error="logoBroken = true"
            />
            <span v-else class="brand-mark footer-mark" aria-hidden="true" />
            <span>{{ siteStore.siteName }}</span>
          </router-link>
          <p>{{ siteStore.siteDescription }}</p>
        </div>

        <div class="footer-col">
          <h4>产品</h4>
          <ul>
            <li v-for="link in productLinks" :key="link.name">
              <router-link :to="{ name: link.name }" class="footer-link">{{ link.label }}</router-link>
            </li>
          </ul>
        </div>

        <div class="footer-col">
          <h4>支持</h4>
          <ul>
            <li v-for="label in supportLinks" :key="label">
              <button type="button" class="footer-link" @click="openBuilding(label)">{{ label }}</button>
            </li>
          </ul>
        </div>

        <div class="footer-col">
          <h4>关于</h4>
          <ul>
            <li v-for="label in aboutLinks" :key="label">
              <button type="button" class="footer-link" @click="openBuilding(label)">{{ label }}</button>
            </li>
          </ul>
        </div>
      </div>

      <div class="footer-bottom">
        <div class="container footer-bottom-inner">
          <span>© {{ currentYear }} {{ siteStore.siteName }} 版权所有</span>
          <a :href="adminUrl" class="admin-entry" title="进入管理后台">
            <el-icon><Setting /></el-icon>
            管理后台
          </a>
        </div>
      </div>
    </footer>

    <el-drawer v-model="mobileMenuOpen" direction="rtl" size="280px" :with-header="false">
      <div class="mobile-menu">
        <router-link to="/" class="mobile-brand">
          <img
            v-if="showLogo"
            :src="siteStore.logoUrl"
            :alt="siteStore.siteName"
            class="mobile-brand-image"
            @error="logoBroken = true"
          />
          <span v-else class="brand-mark" aria-hidden="true" />
          <span>{{ siteStore.siteName }}</span>
        </router-link>

        <nav class="mobile-nav" aria-label="移动端导航">
          <router-link
            v-for="item in navItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="mobile-link"
            :class="{ active: activeNavName === item.name }"
            @click="mobileMenuOpen = false"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="mobile-divider" />

        <template v-if="userStore.isLoggedIn">
          <router-link to="/profile" class="mobile-link" @click="mobileMenuOpen = false">个人中心</router-link>
          <router-link to="/wrong-book" class="mobile-link" @click="mobileMenuOpen = false">我的错题</router-link>
          <router-link to="/interview/records" class="mobile-link" @click="mobileMenuOpen = false">面试记录</router-link>
          <button type="button" class="mobile-link danger" @click="handleCommand('logout')">退出登录</button>
        </template>
        <template v-else>
          <div class="mobile-auth">
            <el-button round @click="router.push({ name: 'login' })">登录</el-button>
            <el-button type="primary" round @click="router.push({ name: 'register' })">注册</el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.portal-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ys-bg);
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  transition: box-shadow 0.2s ease;
}

.nav-container {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 100%;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.2px;
  color: var(--ys-text-primary);
}

.logo-icon {
  font-size: 26px;
}

.logo-image {
  display: block;
  width: auto;
  max-width: 120px;
  height: 32px;
  object-fit: contain;
}

.logo-text {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  padding: 8px 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--ys-text-regular);
  border-radius: var(--ys-radius-md);
  transition: color 0.2s ease, background 0.2s ease;
}

.nav-link:hover {
  color: var(--ys-primary);
  background: rgba(64, 158, 255, 0.06);
}

.nav-link.active {
  color: var(--ys-primary);
  background: rgba(64, 158, 255, 0.1);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: var(--ys-radius-round);
  cursor: pointer;
  outline: none;
  transition: background 0.2s ease;
}

.nav-user:hover {
  background: var(--ys-bg);
}

.nav-avatar {
  background: var(--ys-brand-gradient);
  font-size: 14px;
}

.user-name {
  max-width: 110px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  color: var(--ys-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-caret {
  color: var(--ys-text-secondary);
}

.menu-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  color: var(--ys-text-primary);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: var(--ys-radius-md);
}

.menu-toggle:hover {
  background: var(--ys-bg);
}

.main-content {
  flex: 1;
  padding-top: 64px;
}

.footer {
  color: rgba(255, 255, 255, 0.62);
  background: #1b1d2b;
}

.footer-main {
  display: grid;
  grid-template-columns: 2.2fr 1fr 1fr 1fr;
  gap: 48px;
  padding-top: 56px;
  padding-bottom: 44px;
}

.footer-brand p {
  max-width: 360px;
  margin-top: 16px;
  font-size: 13px;
  line-height: 1.9;
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.footer-mark {
  font-size: 24px;
}

.footer-logo-image {
  display: block;
  width: auto;
  max-width: 140px;
  height: 30px;
  object-fit: contain;
}

.footer-col h4 {
  margin-bottom: 18px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.footer-col ul {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.footer-link {
  padding: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.62);
  text-align: left;
  cursor: pointer;
  background: none;
  border: none;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: #fff;
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.footer-bottom-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 20px;
  padding-bottom: 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.42);
}

.admin-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.42);
  transition: color 0.2s ease;
}

.admin-entry:hover {
  color: rgba(255, 255, 255, 0.82);
}

.mobile-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 4px 24px;
}

.mobile-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px 20px;
  font-size: 19px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.mobile-brand-image {
  display: block;
  width: auto;
  max-width: 130px;
  height: 30px;
  object-fit: contain;
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-link {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 12px 14px;
  font-size: 15px;
  font-weight: 500;
  color: var(--ys-text-regular);
  text-align: left;
  cursor: pointer;
  background: none;
  border: none;
  border-radius: var(--ys-radius-md);
}

.mobile-link:hover {
  color: var(--ys-primary);
  background: var(--ys-bg);
}

.mobile-link.active {
  color: var(--ys-primary);
  background: rgba(64, 158, 255, 0.1);
}

.mobile-link.danger {
  color: var(--ys-danger);
}

.mobile-divider {
  height: 1px;
  margin: 12px 4px;
  background: var(--ys-border-light);
}

.mobile-auth {
  display: flex;
  gap: 12px;
  padding: 8px 4px;
}

.mobile-auth .el-button {
  flex: 1;
  margin: 0;
}

@media (max-width: 960px) {
  .nav-menu,
  .guest-actions {
    display: none;
  }

  .menu-toggle {
    display: flex;
  }

  .nav-container {
    gap: 12px;
  }

  .footer-main {
    grid-template-columns: 1.4fr 1fr 1fr;
    gap: 32px;
  }

  .footer-brand {
    grid-column: 1 / -1;
  }
}

@media (max-width: 600px) {
  .logo-text {
    font-size: 17px;
  }

  .footer-main {
    grid-template-columns: 1fr 1fr;
    padding-top: 36px;
    padding-bottom: 32px;
  }

  .footer-bottom-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
