<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { Check, RefreshLeft, Upload, UserFilled } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import { getSettings, resetSettings, updateSettings } from '@/api/system/setting'
import { uploadImage } from '@/api/system/file'
import { usePermissionStore } from '@/store/permission'
import { useSystemSettingStore } from '@/store/system'
import type { SystemSetting } from '@/types/system'

const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png'])

const permissionStore = usePermissionStore()
const systemSettingStore = useSystemSettingStore()
const canEdit = computed(() => permissionStore.hasPermission('system:setting'))

const loading = ref(false)
const saving = ref(false)
const form = reactive<SystemSetting>({
  siteName: '',
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: ''
})

onMounted(loadSettings)

async function loadSettings(): Promise<void> {
  loading.value = true
  try {
    const data = await getSettings()
    Object.assign(form, data)
    systemSettingStore.apply(data)
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  if (!form.siteName.trim()) {
    ElMessage.warning('请输入平台名称')
    return
  }
  saving.value = true
  try {
    const data = await updateSettings({
      siteName: form.siteName.trim(),
      siteDescription: form.siteDescription.trim(),
      logoUrl: form.logoUrl.trim(),
      defaultAvatar: form.defaultAvatar.trim()
    })
    Object.assign(form, data)
    systemSettingStore.apply(data)
    ElMessage.success('基本信息已保存')
  } finally {
    saving.value = false
  }
}

async function handleReset(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定恢复默认设置吗？当前配置将被覆盖。', '恢复默认设置', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const data = await resetSettings()
  Object.assign(form, data)
  systemSettingStore.apply(data)
  ElMessage.success('已恢复默认设置')
}

async function handleUpload(options: UploadRequestOptions, field: 'logoUrl' | 'defaultAvatar'): Promise<void> {
  const file = options.file as File
  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    ElMessage.warning('仅支持 JPG/PNG 格式的图片')
    return
  }
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.warning('图片大小不能超过 5MB，请压缩后再上传')
    return
  }
  try {
    const result = await uploadImage(file)
    form[field] = result.url
    options.onSuccess?.(result)
    ElMessage.success('图片上传成功')
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    ElMessage.error(
      message && message !== '网络异常，请检查网络后重试'
        ? message
        : '图片上传失败，请确认文件为 JPG/PNG 且不超过 5MB'
    )
  }
}
</script>

<template>
  <PageShell title="系统设置" description="配置平台名称、平台描述、Logo 与默认头像，用于学员端展示。">
    <div v-loading="loading" class="settings-wrap">
      <el-alert
        v-if="!canEdit"
        title="当前账号仅可查看设置，保存与重置需要 system:setting 权限。"
        type="info"
        :closable="false"
        show-icon
        class="readonly-alert"
      />

      <el-form label-position="top" class="settings-form">
        <el-form-item label="平台名称" required>
          <el-input
            v-model="form.siteName"
            maxlength="50"
            show-word-limit
            :disabled="!canEdit"
            placeholder="请输入平台名称"
          />
          <div class="hint">显示在浏览器标题与 Logo 旁</div>
        </el-form-item>

        <el-form-item label="平台描述">
          <el-input
            v-model="form.siteDescription"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :disabled="!canEdit"
            placeholder="请输入平台描述"
          />
        </el-form-item>

        <el-form-item label="Logo">
          <div class="upload-row">
            <div class="preview">
              <img v-if="form.logoUrl" :src="form.logoUrl" alt="Logo 预览" />
              <span v-else class="brand-mark preview-mark" aria-hidden="true" />
            </div>
            <div class="upload-action">
              <el-upload
                :show-file-list="false"
                :disabled="!canEdit"
                accept="image/png,image/jpeg"
                :http-request="(options: UploadRequestOptions) => handleUpload(options, 'logoUrl')"
              >
                <el-button :icon="Upload" :disabled="!canEdit">选择图片</el-button>
              </el-upload>
              <p class="hint">建议 256×256，支持 JPG/PNG，最大 5MB；保存后显示在后台左上角</p>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="默认头像">
          <div class="upload-row">
            <div class="preview is-round">
              <img v-if="form.defaultAvatar" :src="form.defaultAvatar" alt="默认头像预览" />
              <el-icon v-else class="preview-icon"><UserFilled /></el-icon>
            </div>
            <div class="upload-action">
              <el-upload
                :show-file-list="false"
                :disabled="!canEdit"
                accept="image/png,image/jpeg"
                :http-request="(options: UploadRequestOptions) => handleUpload(options, 'defaultAvatar')"
              >
                <el-button :icon="Upload" :disabled="!canEdit">选择图片</el-button>
              </el-upload>
              <p class="hint">用户未设置头像时显示，建议 200×200</p>
            </div>
          </div>
        </el-form-item>

        <div class="settings-actions">
          <el-button v-permission="'system:setting'" :icon="RefreshLeft" @click="handleReset">重置</el-button>
          <el-button
            v-permission="'system:setting'"
            type="primary"
            :icon="Check"
            :loading="saving"
            @click="handleSave"
          >
            保存设置
          </el-button>
        </div>
      </el-form>
    </div>
  </PageShell>
</template>

<style scoped>
.settings-wrap {
  max-width: 760px;
}

.readonly-alert {
  margin-bottom: 20px;
}

.settings-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ys-text-secondary);
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  overflow: hidden;
  border: 1px dashed var(--ys-border);
  border-radius: var(--ys-radius-md);
  background: #fafafa;
}

.preview.is-round {
  border-radius: 50%;
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-mark {
  font-size: 44px;
}

.preview-icon {
  font-size: 40px;
  color: var(--ys-text-placeholder);
}

.upload-action {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
</style>
