<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions
} from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import { changePassword, updateProfile, uploadAvatar } from '@/api/profile'
import { useSiteStore } from '@/store/site'
import { useUserStore } from '@/store/user'
import type { PortalProfile } from '@/types/auth'

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const profile = computed(() => userStore.profile)
const avatarSrc = computed(() => profile.value?.avatar || siteStore.defaultAvatar || undefined)
const initial = computed(() => profile.value?.nickname?.slice(0, 1) || '学')

const loading = ref(false)
const loadFailed = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  nickname: '',
  email: '',
  gender: 0,
  avatar: ''
})

const rules: FormRules = {
  nickname: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 20, message: '用户名不能超过 20 个字符', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const passwordVisible = ref(false)
const passwordSaving = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

watch(
  profile,
  (data) => {
    if (!data) {
      return
    }
    form.nickname = data.nickname
    form.email = data.email ?? ''
    form.gender = data.gender ?? 0
    form.avatar = data.avatar ?? ''
  },
  { immediate: true }
)

onMounted(() => {
  void loadProfile()
})

async function loadProfile(): Promise<void> {
  loading.value = true
  loadFailed.value = false
  try {
    await userStore.fetchProfile()
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  saving.value = true
  try {
    const data = await updateProfile({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      gender: form.gender,
      avatar: form.avatar || ''
    })
    userStore.applyProfile(data)
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function handleAvatarUpload(options: UploadRequestOptions): Promise<void> {
  const file = options.file as File
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG/PNG 格式的图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }

  avatarUploading.value = true
  try {
    const { url } = await uploadAvatar(file)
    form.avatar = url
    const data = await updateProfile({ avatar: url })
    userStore.applyProfile(data)
    ElMessage.success('头像已更新')
  } finally {
    avatarUploading.value = false
  }
}

async function handleChangePassword(): Promise<void> {
  const formInstance = passwordFormRef.value
  if (!formInstance) {
    return
  }
  await formInstance.validate()
  passwordSaving.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordVisible.value = false
    ElMessage.success('密码已修改，请重新登录')
    userStore.reset()
    await router.replace({ name: 'login' })
  } finally {
    passwordSaving.value = false
  }
}

function resetPasswordForm(): void {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  await router.replace({ name: 'home' })
}

/** 后端统一返回 0~100 的正确率 */
function formatRate(value?: number): string {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--'
  }
  return `${value.toFixed(1)}%`
}
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <header class="page-head">
        <div>
          <span class="page-eyebrow">个人中心</span>
          <h1>我的资料</h1>
          <p>维护个人资料，查看练习、面试与错题累计数据。</p>
        </div>
        <el-button class="head-logout" round :icon="SwitchButton" @click="handleLogout">
          退出登录
        </el-button>
      </header>

      <el-skeleton v-if="loading && !profile" :rows="8" animated class="profile-skeleton" />

      <div v-else-if="loadFailed && !profile" class="load-error">
        <span class="error-icon" aria-hidden="true">⚠️</span>
        <p>个人信息加载失败，请稍后重试</p>
        <el-button type="primary" round @click="loadProfile">重新加载</el-button>
      </div>

      <template v-else-if="profile">
        <section class="profile-hero">
          <div class="hero-main">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              :disabled="avatarUploading"
              accept="image/png,image/jpeg"
            >
              <div class="avatar-frame" :class="{ 'avatar-frame--uploading': avatarUploading }">
                <el-avatar :size="90" :src="avatarSrc">{{ initial }}</el-avatar>
                <span class="avatar-mask">
                  <el-icon><Camera /></el-icon>
                  <em>{{ avatarUploading ? '上传中…' : '更换头像' }}</em>
                </span>
              </div>
            </el-upload>

            <div class="hero-info">
              <div class="hero-title">
                <h2>{{ profile.nickname }}</h2>
                <el-tag effect="light" round size="small">学员</el-tag>
              </div>
              <p class="hero-mobile">{{ profile.mobile }}</p>
              <p class="hero-hint">头像支持 JPG/PNG，建议尺寸 200×200</p>
            </div>
          </div>

          <div class="hero-stats">
            <div class="hero-stat">
              <strong>{{ profile.stats.practiceCount }}</strong>
              <span>做题数</span>
            </div>
            <div class="hero-stat">
              <strong>{{ profile.stats.interviewCount }}</strong>
              <span>面试数</span>
            </div>
            <div class="hero-stat">
              <strong>{{ profile.stats.wrongQuestionCount }}</strong>
              <span>错题数</span>
            </div>
          </div>
        </section>

        <div class="profile-grid">
          <section class="panel">
            <header class="panel-head">
              <h2>基本信息</h2>
              <span>手机号不可修改</span>
            </header>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              class="profile-form"
              @submit.prevent="handleSave"
            >
              <el-form-item label="用户名" prop="nickname">
                <el-input
                  v-model="form.nickname"
                  maxlength="20"
                  show-word-limit
                  placeholder="请输入用户名"
                />
              </el-form-item>

              <el-form-item label="手机号">
                <el-input :model-value="profile.mobile" disabled>
                  <template #prefix>
                    <el-icon><Iphone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" maxlength="100" placeholder="选填，用于接收通知" />
              </el-form-item>

              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="form.gender">
                  <el-radio :value="0">保密</el-radio>
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-button type="primary" round :loading="saving" @click="handleSave">
                保存修改
              </el-button>
            </el-form>
          </section>

          <aside class="side-column">
            <section class="panel">
              <header class="panel-head">
                <h2>学习数据</h2>
              </header>
              <div class="data-list">
                <div class="data-row">
                  <span>已完成题目</span>
                  <strong>{{ profile.stats.practiceCount }}</strong>
                </div>
                <div class="data-row">
                  <span>正确率</span>
                  <strong>{{ formatRate(profile.study.accuracyRate) }}</strong>
                </div>
                <div class="data-row">
                  <span>AI 面试次数</span>
                  <strong>{{ profile.stats.interviewCount }}</strong>
                </div>
                <div class="data-row">
                  <span>累计学习天数</span>
                  <strong>{{ profile.study.totalDays }}</strong>
                </div>
              </div>
            </section>

            <section class="panel">
              <header class="panel-head">
                <h2>账号安全</h2>
              </header>
              <div class="security-list">
                <button type="button" class="security-row" @click="passwordVisible = true">
                  <span><el-icon><Lock /></el-icon>修改密码</span>
                  <el-icon><ArrowRight /></el-icon>
                </button>
                <button type="button" class="security-row security-row--danger" @click="handleLogout">
                  <span><el-icon><SwitchButton /></el-icon>退出登录</span>
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>
            </section>
          </aside>
        </div>
      </template>
    </div>

    <el-dialog
      v-model="passwordVisible"
      title="修改密码"
      width="440px"
      align-center
      @closed="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="6~20 位新密码"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            autocomplete="new-password"
            @keyup.enter="handleChangePassword"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="handleChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - 64px);
  padding: 32px 0 64px;
  background:
    radial-gradient(720px 280px at 8% 0%, rgba(102, 126, 234, 0.08), transparent 70%),
    var(--ys-bg);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.page-eyebrow {
  display: inline-block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1px;
  color: #7c89d9;
}

.page-head h1 {
  font-size: 26px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.page-head p {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.head-logout {
  flex: none;
}

.profile-skeleton {
  padding: 28px;
  background: #fff;
  border-radius: 16px;
}

.load-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 24px;
  text-align: center;
  background: #fff;
  border-radius: 16px;
}

.error-icon {
  font-size: 30px;
}

.load-error p {
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.profile-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: 28px 32px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #f3f5ff 0%, #fbf7ff 100%);
  border: 1px solid #e9ecf9;
  border-radius: 18px;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.avatar-uploader {
  flex: none;
}

.avatar-frame {
  position: relative;
  width: 96px;
  height: 96px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(77, 90, 160, 0.16);
}

.avatar-frame .el-avatar {
  display: block;
  width: 90px;
  height: 90px;
  font-size: 30px;
  background: var(--ys-brand-gradient);
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 10px;
  color: #fff;
  background: rgba(23, 26, 43, 0.55);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-mask .el-icon {
  font-size: 18px;
}

.avatar-mask em {
  font-style: normal;
}

.avatar-frame:hover .avatar-mask,
.avatar-frame--uploading .avatar-mask {
  opacity: 1;
}

.hero-info {
  min-width: 0;
}

.hero-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-title h2 {
  overflow: hidden;
  font-size: 22px;
  font-weight: 700;
  color: var(--ys-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-mobile {
  margin-top: 4px;
  font-size: 13px;
  color: var(--ys-text-regular);
}

.hero-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.hero-stats {
  display: flex;
  flex: none;
  gap: 8px;
}

.hero-stat {
  min-width: 92px;
  padding: 14px 18px;
  text-align: center;
  background: rgba(255, 255, 255, 0.86);
  border-radius: 12px;
}

.hero-stat strong {
  display: block;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: #4f5fc4;
}

.hero-stat span {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.85fr);
  gap: 24px;
  align-items: start;
}

.panel {
  padding: 26px;
  background: #fff;
  border: 1px solid #eef0f6;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(46, 56, 110, 0.04);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--ys-border-light);
}

.panel-head h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.panel-head span {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.profile-form {
  max-width: 480px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.data-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 0;
  border-bottom: 1px dashed #eef0f6;
}

.data-row:last-child {
  border-bottom: none;
}

.data-row span {
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.data-row strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 10px;
  font-size: 13px;
  color: var(--ys-text-regular);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 8px;
  transition: background 0.2s ease, color 0.2s ease;
}

.security-row span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.security-row:hover {
  color: var(--ys-primary);
  background: var(--ys-bg);
}

.security-row--danger:hover {
  color: var(--ys-danger);
}

@media (max-width: 1080px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 820px) {
  .profile-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .hero-stats {
    width: 100%;
  }

  .hero-stat {
    flex: 1;
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .profile-page {
    padding: 24px 0 48px;
  }

  .page-head {
    flex-direction: column;
  }

  .page-head h1 {
    font-size: 22px;
  }

  .head-logout {
    width: 100%;
    margin: 0;
  }

  .profile-hero {
    padding: 22px 18px;
  }

  .hero-main {
    gap: 14px;
  }

  .hero-stat {
    padding: 12px 8px;
  }

  .hero-stat strong {
    font-size: 20px;
  }

  .side-column {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 20px 16px;
  }

  .profile-form {
    max-width: none;
  }
}
</style>
