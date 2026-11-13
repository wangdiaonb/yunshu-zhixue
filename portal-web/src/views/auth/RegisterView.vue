<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { sendSmsCode } from '@/api/auth'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()
const { remaining, start } = useCountdown(60)

const formRef = ref<FormInstance>()
const loading = ref(false)
const agreed = ref(false)
const form = reactive({
  mobile: '',
  code: '',
  password: '',
  confirmPassword: ''
})

/** 密码强度：长度 ≥6、含大小写字母、含数字与特殊字符逐项加分 */
const passwordStrength = computed(() => {
  const password = form.password
  if (!password) {
    return 0
  }
  let score = 0
  if (password.length >= 6) {
    score += 1
  }
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) {
    score += 1
  }
  if (/\d/.test(password) && /[^A-Za-z0-9]/.test(password)) {
    score += 1
  }
  return score
})

const strengthLabel = computed(() => ['', '弱', '中', '强'][passwordStrength.value])

const rules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function handleSendCode(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validateField('mobile')
  const result = await sendSmsCode({ mobile: form.mobile, type: 'register' })
  start()
  if (result.debugCode) {
    form.code = result.debugCode
    ElMessage.success(`验证码已发送，开发环境已自动填入：${result.debugCode}`)
    return
  }
  ElMessage.success('验证码已发送')
}

function showAgreement(name: string): void {
  ElMessage.info(`${name}页面建设中，敬请期待`)
}

async function submit(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意《用户协议》和《隐私政策》')
    return
  }
  loading.value = true
  try {
    await userStore.registerAccount(
      { mobile: form.mobile, code: form.code, password: form.password },
      false
    )
    ElMessage.success(`注册成功，欢迎加入${siteStore.siteName}`)
    await router.replace({ name: 'profile' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="创建账号" subtitle="注册后即可保存练习记录、错题本与面试报告">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="form.mobile" placeholder="请输入手机号" autocomplete="tel" />
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <div class="code-row">
          <el-input v-model="form.code" placeholder="6 位验证码" maxlength="6" />
          <el-button :disabled="remaining > 0" @click="handleSendCode">
            {{ remaining > 0 ? `${remaining}s 后重发` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="6~20 位密码"
          autocomplete="new-password"
        />
        <div
          v-if="form.password"
          class="strength-meter"
          :class="`strength-${passwordStrength}`"
        >
          <span class="strength-bars">
            <i
              v-for="index in 3"
              :key="index"
              class="strength-bar"
              :class="{ active: index <= passwordStrength }"
            />
          </span>
          <em class="strength-label">{{ strengthLabel }}</em>
        </div>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入密码"
          autocomplete="new-password"
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-checkbox v-model="agreed" class="agreement">
        我已阅读并同意
        <span class="agreement-link" @click.stop="showAgreement('用户协议')">《用户协议》</span>
        和
        <span class="agreement-link" @click.stop="showAgreement('隐私政策')">《隐私政策》</span>
      </el-checkbox>
      <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">注册</el-button>
    </el-form>

    <p class="switch-line">
      已有账号？
      <router-link to="/login">直接登录</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
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

.strength-meter {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  margin-top: 8px;
}

.strength-bars {
  display: flex;
  flex: 1;
  gap: 4px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: #eef0f6;
  border-radius: 2px;
  transition: background 0.2s ease;
}

.strength-1 .strength-bar.active {
  background: var(--ys-danger);
}

.strength-2 .strength-bar.active {
  background: var(--ys-warning);
}

.strength-3 .strength-bar.active {
  background: var(--ys-success);
}

.strength-label {
  flex: none;
  width: 16px;
  font-size: 12px;
  font-style: normal;
  text-align: right;
  color: var(--ys-text-secondary);
}

.strength-1 .strength-label {
  color: var(--ys-danger);
}

.strength-2 .strength-label {
  color: var(--ys-warning);
}

.strength-3 .strength-label {
  color: var(--ys-success);
}

.agreement {
  display: flex;
  align-items: flex-start;
  width: 100%;
  height: auto;
  margin-bottom: 16px;
  font-size: 12px;
  line-height: 1.6;
  white-space: normal;
}

.agreement :deep(.el-checkbox__label) {
  color: var(--ys-text-secondary);
  white-space: normal;
}

.agreement-link {
  color: var(--ys-primary);
  cursor: pointer;
}

.agreement-link:hover {
  text-decoration: underline;
}

.switch-line {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--ys-text-secondary);
}
</style>
