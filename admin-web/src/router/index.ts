import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { adminRoutes } from './routes'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      ...adminRoutes,
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/error/ForbiddenView.vue'),
        meta: { title: '无权访问' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const appTitle = import.meta.env.VITE_APP_TITLE
  document.title = to.meta.title ? `${to.meta.title} - ${appTitle}` : appTitle

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (!to.meta.public && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && userStore.isLoggedIn) {
    return { name: 'dashboard' }
  }
  if (to.meta.superAdminOnly && !permissionStore.isSuperAdmin) {
    return { name: 'forbidden' }
  }
  if (to.meta.permission && !permissionStore.hasPermission(to.meta.permission)) {
    return { name: 'forbidden' }
  }
  return true
})

export default router
