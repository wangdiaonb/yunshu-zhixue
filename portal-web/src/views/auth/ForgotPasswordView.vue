<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '@/components/AuthShell.vue'
import { resetPassword, sendSmsCode, verifyResetCode } from '@/api/auth'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()
const { remaining, start } = useCountdown(60)

const step = ref<1 | 2>(1)
const loading = ref(false)
const verifyFormRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()
const resetToken = ref('')

const verifyForm = reactive({ mobile: '', code: '' })
const resetForm = reactive({ newPassword: '', confirmPassword: '' })

const verifyRules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

const resetRules: FormRules = {
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== resetForm.newPassword) {
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
  const formInstance = verifyFormRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validateField('mobile')
  await sendSmsCode({ mobile: verifyForm.mobile, type: 'reset' })
  ElMessage.success('验证码已发送')
  start()
}

async function handleVerify(): Promise<void> {
  const formInstance = verifyFormRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  loading.value = true
  try {
    const result = await verifyResetCode({ ...verifyForm })
    resetToken.value = result.resetToken
    step.value = 2
  } finally {
    loading.value = false
  }
}

async function handleReset(): Promise<void> {
  const formInstance = resetFormRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  loading.value = true
  try {
    await resetPassword({ resetToken: resetToken.value, newPassword: resetForm.newPassword })
    ElMessage.success('密码已重置，请使用新密码登录')
    await router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="找回密码" subtitle="通过注册手机号验证身份后重置密码">
    <el-steps :active="step" simple class="steps">
      <el-step title="验证手机号" />
      <el-step title="设置新密码" />
    </el-steps>

    <el-form
      v-if="step === 1"
      ref="verifyFormRef"
      :model="verifyForm"
      :rules="verifyRules"
      label-position="top"
      @submit.prevent="handleVerify"
    >
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="verifyForm.mobile" placeholder="请输入注册手机号" />
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <div class="code-row">
          <el-input v-model="verifyForm.code" placeholder="6 位验证码" maxlength="6" />
          <el-button :disabled="remaining > 0" @click="handleSendCode">
            {{ remaining > 0 ? `${remaining}s 后重发` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>
      <el-button type="primary" class="submit-btn" :loading="loading" @click="handleVerify">
        下一步
      </el-button>
    </el-form>

    <el-form
      v-else
      ref="resetFormRef"
      :model="resetForm"
      :rules="resetRules"
      label-position="top"
      @submit.prevent="handleReset"
    >
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="6~20 位新密码" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="resetForm.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
          @keyup.enter="handleReset"
        />
      </el-form-item>
      <el-button type="primary" class="submit-btn" :loading="loading" @click="handleReset">
        重置密码
      </el-button>
      <el-button text class="back-btn" @click="step = 1">返回上一步</el-button>
    </el-form>

    <p class="switch-line">
      想起密码了？
      <router-link to="/login">返回登录</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
.steps {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
}

.back-btn {
  width: 100%;
  margin-top: 8px;
  margin-left: 0;
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
