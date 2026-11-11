<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { sendSmsCode } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { useCountdown } from '@/composables/useCountdown'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { remaining, start } = useCountdown(60)

const activeTab = ref<'password' | 'sms'>('password')
const loading = ref(false)
const remember = ref(true)
const passwordFormRef = ref<FormInstance>()
const smsFormRef = ref<FormInstance>()

const passwordForm = reactive({ account: '', password: '' })
const smsForm = reactive({ mobile: '', code: '' })

const passwordRules: FormRules = {
  account: [{ required: true, message: '请输入手机号或邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ]
}

const smsRules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : '/'))

async function submitPassword(): Promise<void> {
  const form = passwordFormRef.value
  if (!form) {
    return
  }
  await form.validate()
  loading.value = true
  try {
    await userStore.loginPassword({ ...passwordForm }, remember.value)
    ElMessage.success('登录成功')
    await router.replace(redirect.value)
  } finally {
    loading.value = false
  }
}

async function submitSms(): Promise<void> {
  const form = smsFormRef.value
  if (!form) {
    return
  }
  await form.validate()
  loading.value = true
  try {
    await userStore.loginSms({ ...smsForm }, remember.value)
    ElMessage.success('登录成功')
    await router.replace(redirect.value)
  } finally {
    loading.value = false
  }
}

async function handleSendCode(): Promise<void> {
  const form = smsFormRef.value
  if (!form) {
    return
  }
  await form.validateField('mobile')
  await sendSmsCode({ mobile: smsForm.mobile, type: 'login' })
  ElMessage.success('验证码已发送')
  start()
}
</script>

<template>
  <AuthShell title="登录云枢智学" subtitle="登录后可刷题、记录错题、发起 AI 模拟面试">
    <el-tabs v-model="activeTab" stretch>
      <el-tab-pane label="密码登录" name="password">
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          @submit.prevent="submitPassword"
        >
          <el-form-item label="手机号 / 邮箱" prop="account">
            <el-input v-model="passwordForm.account" placeholder="请输入手机号或邮箱" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="passwordForm.password"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submitPassword"
            />
          </el-form-item>
          <div class="form-extra">
            <el-checkbox v-model="remember">记住我</el-checkbox>
            <router-link to="/forgot-password">忘记密码？</router-link>
          </div>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submitPassword">
            登录
          </el-button>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="验证码登录" name="sms">
        <el-form
          ref="smsFormRef"
          :model="smsForm"
          :rules="smsRules"
          label-position="top"
          @submit.prevent="submitSms"
        >
          <el-form-item label="手机号" prop="mobile">
            <el-input v-model="smsForm.mobile" placeholder="请输入手机号" autocomplete="tel" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="smsForm.code" placeholder="6 位验证码" maxlength="6" />
              <el-button :disabled="remaining > 0" @click="handleSendCode">
                {{ remaining > 0 ? `${remaining}s 后重发` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submitSms">
            登录
          </el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <p class="switch-line">
      还没有账号？
      <router-link to="/register">立即注册</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.switch-line {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--ys-text-secondary);
}
</style>
