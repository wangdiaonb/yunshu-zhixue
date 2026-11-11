<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)
const form = reactive({ account: '', password: '' })

const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ]
}

const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'))
const currentYear = new Date().getFullYear()

async function submit(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  loading.value = true
  try {
    await userStore.login({ ...form }, remember.value)
    ElMessage.success('登录成功')
    await router.replace(redirect.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="bg-grid" aria-hidden="true" />
    <span class="bg-orb bg-orb--left" aria-hidden="true" />
    <span class="bg-orb bg-orb--right" aria-hidden="true" />

    <main class="login-shell">
      <section class="login-card">
        <header class="card-brand">
          <span class="brand-mark brand-icon" aria-hidden="true" />
          <div class="brand-copy">
            <strong>云枢智学</strong>
            <span>AI 赋能的智能面试训练平台</span>
          </div>
        </header>

        <div class="card-divider" />

        <div class="card-header">
          <span class="card-eyebrow">管理后台</span>
          <h1>登录后台</h1>
          <p>请使用管理员账号登录，登录态与学员端相互独立</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item label="账号" prop="account">
            <el-input v-model="form.account" placeholder="用户名或手机号" autocomplete="username" autofocus>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submit"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-extra">
            <el-checkbox v-model="remember">记住登录状态</el-checkbox>
            <span class="form-hint">连续 5 次密码错误将锁定 5 分钟</span>
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <footer class="card-footer">
          <router-link to="/">返回学员端首页</router-link>
        </footer>
      </section>

      <p class="page-footer">© {{ currentYear }} 云枢智学 · 管理后台</p>
    </main>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 48px 20px;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 16% 18%, rgba(102, 126, 234, 0.24) 0%, transparent 42%),
    radial-gradient(circle at 84% 82%, rgba(118, 75, 162, 0.24) 0%, transparent 46%),
    linear-gradient(135deg, #151622 0%, #1d1e2c 48%, #24253d 100%);
}

.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 48px 48px;
  -webkit-mask-image: radial-gradient(circle at center, #000 0%, transparent 72%);
  mask-image: radial-gradient(circle at center, #000 0%, transparent 72%);
  pointer-events: none;
}

.bg-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.35;
  pointer-events: none;
}

.bg-orb--left {
  top: 8%;
  left: -120px;
  width: 420px;
  height: 420px;
  background: #5b6cf0;
  animation: orb-float 14s ease-in-out infinite;
}

.bg-orb--right {
  right: -100px;
  bottom: 4%;
  width: 380px;
  height: 380px;
  background: #8b5cf6;
  animation: orb-float 18s ease-in-out infinite reverse;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 440px;
}

.login-card {
  width: 100%;
  padding: 32px 34px 24px;
  background: #fff;
  border-radius: 18px;
  box-shadow:
    0 28px 70px rgba(0, 0, 0, 0.35),
    0 4px 16px rgba(0, 0, 0, 0.1);
  animation: card-in 0.45s ease both;
}

.card-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  font-size: 38px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.brand-copy strong {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--ys-text-primary);
}

.brand-copy span {
  margin-top: 2px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.card-divider {
  height: 1px;
  margin: 20px 0 22px;
  background: var(--ys-border-light);
}

.card-header {
  margin-bottom: 22px;
}

.card-eyebrow {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: var(--ys-primary);
  font-size: 11px;
  letter-spacing: 1px;
}

.card-header h1 {
  margin-top: 12px;
  font-size: 25px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.card-header p {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ys-text-secondary);
}

.login-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-card :deep(.el-form-item__label) {
  font-size: 13px;
  color: var(--ys-text-regular);
}

.login-card :deep(.el-input__wrapper) {
  padding: 4px 13px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--ys-border) inset;
  transition: box-shadow 0.2s ease;
}

.login-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--ys-primary-light) inset;
}

.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--ys-primary) inset,
    0 0 0 3px rgba(64, 158, 255, 0.12);
}

.login-card :deep(.el-input__prefix) {
  margin-right: 6px;
  color: var(--ys-text-secondary);
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 2px 0 18px;
}

.form-hint {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.submit-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  background: var(--ys-brand-gradient);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
}

.submit-btn:hover {
  opacity: 0.94;
}

.card-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--ys-border-light);
  text-align: center;
  font-size: 13px;
}

.page-footer {
  margin-top: 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.38);
  letter-spacing: 0.5px;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes orb-float {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(22px, -26px, 0);
  }
}

@media (max-height: 760px) {
  .login-page {
    padding: 24px 20px;
  }

  .login-card {
    padding: 24px 30px 18px;
  }

  .card-divider {
    margin: 14px 0 16px;
  }

  .card-header {
    margin-bottom: 16px;
  }

  .page-footer {
    margin-top: 14px;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 28px 16px;
  }

  .login-card {
    padding: 26px 22px 20px;
    border-radius: 16px;
  }

  .card-header h1 {
    font-size: 23px;
  }

  .form-hint {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card,
  .bg-orb--left,
  .bg-orb--right {
    animation: none;
  }
}
</style>
