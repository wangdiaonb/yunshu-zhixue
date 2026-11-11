<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, type RouteLocationRaw } from 'vue-router'
import { ElCollapseTransition, ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowRight, Expand, Fold, Monitor } from '@element-plus/icons-vue'
import { adminRoutes, MENU_GROUPS } from '@/router/routes'
import { usePermissionStore } from '@/store/permission'
import { useSystemSettingStore } from '@/store/system'
import { useUserStore } from '@/store/user'
import { useMediaQuery } from '@/composables/useMediaQuery'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const systemSettingStore = useSystemSettingStore()

const logoBroken = ref(false)
const brandLogo = computed(() => systemSettingStore.settings.logoUrl)
const brandTitle = computed(() => {
  const siteName = systemSettingStore.settings.siteName?.trim()
  if (!siteName) {
    return '云枢智学后台'
  }
  return siteName.endsWith('后台') ? siteName : `${siteName}后台`
})

watch(
  () => systemSettingStore.settings.logoUrl,
  () => {
    logoBroken.value = false
  }
)

onMounted(() => {
  void systemSettingStore.ensureLoaded().catch(() => undefined)
})

const COLLAPSE_KEY = 'yszx_admin_sidebar_collapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
const mobileOpen = ref(false)
/** 当前展开的一级菜单（title 集合，允许同时展开多个） */
const openGroups = ref<string[]>([])
const isMobile = useMediaQuery('(max-width: 960px)')

/** 桌面端：折叠后只显示图标；移动端：侧边栏以抽屉形式出现 */
const isCollapsedView = computed(() => !isMobile.value && collapsed.value)
const sidebarVisible = computed(() => (isMobile.value ? mobileOpen.value : !collapsed.value))

watch(collapsed, (value) => {
  localStorage.setItem(COLLAPSE_KEY, value ? '1' : '0')
})

watch(isMobile, (mobile) => {
  if (!mobile) {
    mobileOpen.value = false
  }
})

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
    // 进入页面时自动展开其所属一级菜单，避免当前页面被折叠隐藏
    const group = route.meta.group
    if (group && !openGroups.value.includes(group)) {
      openGroups.value = [...openGroups.value, group]
    }
  },
  { immediate: true }
)

const menuGroups = computed(() =>
  MENU_GROUPS.map((group) => ({
    title: group.title,
    icon: group.icon,
    items: adminRoutes
      .filter((item) => item.meta?.group === group.title)
      .filter((item) => !item.meta?.superAdminOnly || permissionStore.isSuperAdmin)
      .filter((item) => permissionStore.hasPermission(item.meta?.permission))
      .map((item) => ({
        name: item.name as string,
        path: `/${item.path}`,
        title: item.meta?.title ?? '',
        icon: item.meta?.icon ?? '•'
      }))
  })).filter((group) => group.items.length > 0)
)

function isGroupOpen(title: string): boolean {
  return openGroups.value.includes(title)
}

/**
 * 点击一级菜单切换二级菜单：
 * 侧边栏折叠时先展开侧边栏，再打开对应一级目录。
 */
function toggleGroup(title: string): void {
  if (isCollapsedView.value) {
    collapsed.value = false
    if (!isGroupOpen(title)) {
      openGroups.value = [...openGroups.value, title]
    }
    return
  }
  openGroups.value = isGroupOpen(title)
    ? openGroups.value.filter((item) => item !== title)
    : [...openGroups.value, title]
}

interface BreadcrumbItem {
  title: string
  to?: RouteLocationRaw
}

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const items: BreadcrumbItem[] = [{ title: '首页', to: { name: 'dashboard' } }]
  if (route.meta.group && route.meta.group !== '概览') {
    items.push({ title: route.meta.group })
  }
  if (route.meta.title) {
    items.push({ title: route.meta.title })
  }
  return items
})

const userInitial = computed(() => userStore.displayName.slice(0, 1).toUpperCase())
const roleName = computed(
  () => userStore.adminUser?.roleName || (permissionStore.isSuperAdmin ? '超级管理员' : '管理员')
)
const currentYear = new Date().getFullYear()

function toggleSidebar(): void {
  if (isMobile.value) {
    mobileOpen.value = !mobileOpen.value
    return
  }
  collapsed.value = !collapsed.value
}

function openPortal(): void {
  // 生产环境 / 为学员端入口（D20 同域分区）
  window.open('/', '_blank', 'noopener,noreferrer')
}

async function handleCommand(command: string): Promise<void> {
  if (command === 'portal') {
    openPortal()
    return
  }
  if (command !== 'logout') {
    return
  }
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="admin-layout" :class="{ 'is-collapsed': isCollapsedView }">
    <aside class="admin-sidebar" :class="{ 'is-open': isMobile && mobileOpen }">
      <div class="sidebar-brand">
        <router-link :to="{ name: 'dashboard' }" class="brand-link">
          <img
            v-if="brandLogo && !logoBroken"
            :src="brandLogo"
            class="brand-logo"
            alt="平台 Logo"
            @error="logoBroken = true"
          />
          <span v-else class="brand-mark brand-icon" aria-hidden="true" />
          <span v-show="!isCollapsedView" class="brand-text">{{ brandTitle }}</span>
        </router-link>
        <button
          v-if="isMobile"
          type="button"
          class="brand-close"
          aria-label="关闭菜单"
          @click="mobileOpen = false"
        >
          ×
        </button>
      </div>

      <el-scrollbar class="sidebar-scroll">
        <nav class="sidebar-nav">
          <div v-for="group in menuGroups" :key="group.title" class="menu-group">
            <button
              type="button"
              class="menu-group__header"
              :aria-expanded="isGroupOpen(group.title)"
              :aria-label="group.title"
              :title="isCollapsedView ? group.title : undefined"
              @click="toggleGroup(group.title)"
            >
              <span class="menu-group__icon">{{ group.icon }}</span>
              <span v-show="!isCollapsedView" class="menu-group__title">{{ group.title }}</span>
              <el-icon
                v-show="!isCollapsedView"
                class="menu-group__arrow"
                :class="{ 'is-open': isGroupOpen(group.title) }"
              >
                <ArrowRight />
              </el-icon>
            </button>

            <el-collapse-transition>
              <div v-show="!isCollapsedView && isGroupOpen(group.title)" class="menu-children">
                <router-link
                  v-for="item in group.items"
                  :key="item.name"
                  :to="item.path"
                  class="menu-item"
                  :class="{ 'is-active': route.name === item.name }"
                  :aria-current="route.name === item.name ? 'page' : undefined"
                >
                  <span class="menu-item__icon">{{ item.icon }}</span>
                  <span class="menu-item__label">{{ item.title }}</span>
                </router-link>
              </div>
            </el-collapse-transition>
          </div>
        </nav>
      </el-scrollbar>

      <div class="sidebar-footer">
        <button
          v-if="!isMobile"
          type="button"
          class="collapse-btn"
          :aria-label="collapsed ? '展开菜单' : '收起菜单'"
          @click="collapsed = !collapsed"
        >
          <span>{{ collapsed ? '»' : '«' }}</span>
          <span v-show="!isCollapsedView">收起菜单</span>
        </button>
        <p v-show="!isCollapsedView" class="sidebar-version">云枢智学 · v0.1.0</p>
      </div>
    </aside>

    <div v-if="isMobile && mobileOpen" class="sidebar-mask" @click="mobileOpen = false" />

    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-tooltip :content="sidebarVisible ? '收起菜单' : '展开菜单'" placement="bottom">
            <button type="button" class="header-icon-btn" @click="toggleSidebar">
              <el-icon :size="18">
                <Fold v-if="sidebarVisible" />
                <Expand v-else />
              </el-icon>
            </button>
          </el-tooltip>

          <el-breadcrumb separator="/">
            <template v-for="(item, index) in breadcrumbs" :key="`${item.title}-${index}`">
              <!-- 移动端只保留当前页标题，避免顶栏挤压 -->
              <el-breadcrumb-item
                v-if="!isMobile || index === breadcrumbs.length - 1"
                :to="index === breadcrumbs.length - 1 ? undefined : item.to"
              >
                {{ item.title }}
              </el-breadcrumb-item>
            </template>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="查看学员端" placement="bottom">
            <button type="button" class="header-icon-btn" @click="openPortal">
              <el-icon :size="17"><Monitor /></el-icon>
            </button>
          </el-tooltip>

          <span class="header-divider" aria-hidden="true" />

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="header-user">
              <el-avatar :size="32" :src="userStore.adminUser?.avatar || undefined">
                {{ userInitial }}
              </el-avatar>
              <div class="header-user__meta">
                <strong>{{ userStore.displayName }}</strong>
                <span>{{ roleName }}</span>
              </div>
              <el-icon class="header-user__arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="portal">返回学员端</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>

      <footer class="admin-footer">
        <span>云枢智学 · 管理后台</span>
        <span>© {{ currentYear }}</span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--ys-bg);
}

/* ========== 侧边栏 ========== */
.admin-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  width: var(--ys-sidebar-width);
  color: #fff;
  background: linear-gradient(180deg, #1d1e2c 0%, #23243a 100%);
  transition: width 0.25s ease, transform 0.25s ease;
}

.is-collapsed .admin-sidebar {
  width: var(--ys-sidebar-collapsed-width);
}

.sidebar-brand {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--ys-header-height);
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  white-space: nowrap;
}

.brand-icon {
  font-size: 24px;
}

.brand-logo {
  flex: none;
  width: 30px;
  height: 30px;
  padding: 2px;
  object-fit: contain;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  box-sizing: border-box;
}

.brand-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-close {
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.is-collapsed .sidebar-brand {
  justify-content: center;
  padding: 0;
}

.sidebar-scroll {
  flex: 1;
  min-height: 0;
}

.sidebar-scroll :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.sidebar-nav {
  padding: 16px 12px;
}

.menu-group {
  margin-bottom: 6px;
}

.menu-group__header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: var(--ys-radius-md);
  background: transparent;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.menu-group__header:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}

.menu-group__icon {
  width: 24px;
  flex: none;
  text-align: center;
  font-size: 17px;
}

.menu-group__title {
  flex: 1;
  letter-spacing: 0.5px;
}

.menu-group__arrow {
  flex: none;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  transition: transform 0.2s ease;
}

.menu-group__arrow.is-open {
  transform: rotate(90deg);
}

.menu-children {
  margin: 2px 0 8px 14px;
  padding-left: 8px;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  margin-bottom: 2px;
  border-radius: var(--ys-radius-md);
  color: rgba(255, 255, 255, 0.72);
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease;
}

.menu-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.menu-item.is-active {
  color: #fff;
  background: var(--ys-brand-gradient);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.28);
}

.menu-item__icon {
  width: 24px;
  flex: none;
  text-align: center;
  font-size: 17px;
}

.menu-item__label {
  font-size: 13px;
}

.is-collapsed .menu-group {
  margin-bottom: 4px;
}

.is-collapsed .menu-group__header {
  justify-content: center;
  padding: 11px 0;
}

.is-collapsed .menu-group + .menu-group {
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.sidebar-footer {
  flex: none;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 8px;
  border: none;
  border-radius: var(--ys-radius-md);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.sidebar-version {
  margin-top: 10px;
  text-align: center;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.28);
}

/* ========== 主区域 ========== */
.admin-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
  margin-left: var(--ys-sidebar-width);
  transition: margin-left 0.25s ease;
}

.is-collapsed .admin-main {
  margin-left: var(--ys-sidebar-collapsed-width);
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: var(--ys-header-height);
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--ys-border-light);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left {
  min-width: 0;
}

.header-left :deep(.el-breadcrumb) {
  min-width: 0;
  overflow: hidden;
}

.header-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: var(--ys-radius-md);
  background: transparent;
  color: var(--ys-text-regular);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.header-icon-btn:hover {
  color: var(--ys-primary);
  background: var(--ys-bg);
}

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--ys-border-light);
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: var(--ys-radius-round);
  cursor: pointer;
  transition: background 0.2s ease;
}

.header-user:hover {
  background: var(--ys-bg);
}

.header-user__meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.header-user__meta strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.header-user__meta span {
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.header-user__arrow {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.admin-content {
  flex: 1;
  min-width: 0;
  padding: 24px;
  overflow-x: hidden;
}

.admin-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  font-size: 12px;
  color: var(--ys-text-secondary);
  border-top: 1px solid var(--ys-border-light);
  background: #fff;
}

.sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
}

/* ========== 页面切换动画 ========== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ========== 响应式：侧边栏抽屉 ========== */
@media (max-width: 960px) {
  .admin-sidebar,
  .is-collapsed .admin-sidebar {
    width: var(--ys-sidebar-width);
    transform: translateX(-100%);
  }

  .admin-sidebar.is-open {
    transform: translateX(0);
    box-shadow: var(--ys-shadow-lg);
  }

  .admin-main,
  .is-collapsed .admin-main {
    margin-left: 0;
  }

  .admin-content {
    padding: 16px;
  }

  .admin-header {
    padding: 0 16px;
  }

  .header-left,
  .header-right {
    gap: 8px;
  }

  /* 移动端顶栏只保留用户入口，查看前台走用户下拉菜单 */
  .header-right .header-icon-btn,
  .header-divider {
    display: none;
  }

  .header-user__meta {
    display: none;
  }

  .admin-footer {
    padding: 12px 16px;
  }
}
</style>
