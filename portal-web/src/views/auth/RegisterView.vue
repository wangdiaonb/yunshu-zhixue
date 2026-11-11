<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { sendSmsCode } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()
const userStore = useUserStore()
const { remaining, start } = useCountdown(60)

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  mobile: '',
  code: '',
  password: '',
  confirmPassword: ''
})

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
  await sendSmsCode({ mobile: form.mobile, type: 'register' })
  ElMessage.success('验证码已发送')
  start()
}

async function submit(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  loading.value = true
  try {
    await userStore.registerAccount(
      { mobile: form.mobile, code: form.code, password: form.password },
      false
    )
    ElMessage.success('注册成功，欢迎加入云枢智学')
    await router.replace('/')
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

.switch-line {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--ys-text-secondary);
}
</style>
