import type { RouteRecordRaw } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 菜单图标（沿用原型的表情图标） */
    icon?: string
    /** 菜单分组标题 */
    group?: string
    /** 访问所需权限码（9 组 34 项） */
    permission?: string
    /** 仅超级管理员可访问（角色管理，D4-5） */
    superAdminOnly?: boolean
    /** 公开页面（登录页） */
    public?: boolean
  }
}

/**
 * 管理端页面路由：同时作为侧边菜单的数据源，避免菜单与路由两处维护。
 */
export const adminRoutes: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'dashboard',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { title: '数据看板', icon: '📊', group: '概览', permission: 'dashboard:view' }
  },
  {
    path: 'content/subjects',
    name: 'subjects',
    component: () => import('@/views/content/SubjectsView.vue'),
    meta: { title: '学科管理', icon: '🎓', group: '内容管理', permission: 'subject:view' }
  },
  {
    path: 'content/courses',
    name: 'courses',
    component: () => import('@/views/content/CoursesView.vue'),
    meta: { title: 'B站课程库', icon: '🎬', group: '内容管理', permission: 'course:view' }
  },
  {
    path: 'content/questions',
    name: 'questions',
    component: () => import('@/views/content/QuestionsView.vue'),
    meta: { title: '题库管理', icon: '📚', group: '内容管理', permission: 'question:view' }
  },
  {
    path: 'content/interview-bank',
    name: 'interview-bank',
    component: () => import('@/views/content/InterviewBankView.vue'),
    meta: { title: '面试题库管理', icon: '🗂️', group: '内容管理', permission: 'interviewbank:view' }
  },
  {
    path: 'content/routes',
    name: 'learning-routes',
    component: () => import('@/views/content/RoutesView.vue'),
    meta: { title: '学习路线', icon: '🗺️', group: '内容管理', permission: 'route:view' }
  },
  {
    path: 'students',
    name: 'students',
    component: () => import('@/views/student/StudentsView.vue'),
    meta: { title: '学员管理', icon: '🎓', group: '学员管理', permission: 'student:view' }
  },
  {
    path: 'system/users',
    name: 'system-users',
    component: () => import('@/views/system/UsersView.vue'),
    meta: { title: '用户管理', icon: '👥', group: '系统管理', permission: 'user:view' }
  },
  {
    path: 'system/roles',
    name: 'system-roles',
    component: () => import('@/views/system/RolesView.vue'),
    meta: { title: '角色管理', icon: '🔐', group: '系统管理', superAdminOnly: true }
  },
  {
    path: 'system/logs',
    name: 'system-logs',
    component: () => import('@/views/system/LogsView.vue'),
    meta: { title: '操作日志', icon: '📝', group: '系统管理', permission: 'system:log_view' }
  },
  {
    path: 'system/settings',
    name: 'system-settings',
    component: () => import('@/views/system/SettingsView.vue'),
    meta: { title: '系统设置', icon: '⚙️', group: '系统管理', permission: 'system:setting' }
  }
]

export interface MenuGroupMeta {
  title: string
  /** 一级菜单图标（侧边栏折叠时仍显示） */
  icon: string
}

/** 一级菜单（分组）顺序与图标，二级菜单由路由 meta.group 归属 */
export const MENU_GROUPS: MenuGroupMeta[] = [
  { title: '概览', icon: '📊' },
  { title: '内容管理', icon: '🗂️' },
  { title: '学员管理', icon: '🎓' },
  { title: '系统管理', icon: '⚙️' }
]
