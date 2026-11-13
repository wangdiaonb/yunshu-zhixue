import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 公开页面：未登录可访问（题库浏览开放，D4-2） */
    public?: boolean
    /** 仅未登录可访问（登录/注册/找回密码） */
    guestOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/PortalLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页', public: true }
      },
      {
        path: 'questions',
        name: 'questions',
        component: () => import('@/views/questions/QuestionsView.vue'),
        meta: { title: '题库中心', public: true }
      },
      {
        path: 'practice',
        name: 'practice',
        component: () => import('@/views/practice/PracticeView.vue'),
        meta: { title: '刷题练习' }
      },
      {
        path: 'wrong-book',
        name: 'wrong-book',
        component: () => import('@/views/wrong-book/WrongBookView.vue'),
        meta: { title: '错题本' }
      },
      {
        path: 'routes',
        name: 'routes',
        component: () => import('@/views/routes/RoutesView.vue'),
        meta: { title: '学习路线' }
      },
      {
        path: 'interview',
        name: 'interview-config',
        component: () => import('@/views/interview/InterviewConfigView.vue'),
        meta: { title: 'AI 面试' }
      },
      {
        path: 'interview/session',
        name: 'interview-session',
        component: () => import('@/views/interview/InterviewSessionView.vue'),
        meta: { title: 'AI 面试进行中' }
      },
      {
        path: 'interview/records',
        name: 'interview-records',
        component: () => import('@/views/interview/InterviewRecordsView.vue'),
        meta: { title: '面试记录' }
      },
      {
        path: 'interview/report/:id',
        name: 'interview-report',
        component: () => import('@/views/interview/InterviewReportView.vue'),
        meta: { title: '面试报告' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', public: true, guestOnly: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', public: true, guestOnly: true }
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { title: '找回密码', public: true, guestOnly: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && userStore.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
