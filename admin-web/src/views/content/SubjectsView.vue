<script setup lang="ts">
import { computed, markRaw, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Bottom,
  Check,
  Collection,
  Delete,
  Document,
  Edit,
  Files,
  FolderOpened,
  Guide,
  Plus,
  Top,
  VideoCamera
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  createStage,
  createSubject,
  deleteStage,
  deleteSubject,
  getSubjectStats,
  listSubjects,
  sortStages,
  sortSubjects,
  updateStage,
  updateStageStatus,
  updateSubject
} from '@/api/subject'
import type {
  StageItem,
  StageSavePayload,
  SubjectCard,
  SubjectSavePayload,
  SubjectStats
} from '@/types/subject'

const themeColors = ['#667eea', '#4facfe', '#43e97b', '#fa709a', '#f6d365', '#a18cd1', '#f093fb', '#30cfd0']

const loading = ref(false)
const stats = ref<SubjectStats>({
  totalSubjects: 0,
  subjectsWithStages: 0,
  totalStages: 0,
  totalQuestions: 0
})
const subjects = ref<SubjectCard[]>([])

const statCards = computed(() => [
  { label: '总学科数', value: stats.value.totalSubjects, icon: markRaw(Collection), tone: 'blue' },
  { label: '有阶段的学科', value: stats.value.subjectsWithStages, icon: markRaw(FolderOpened), tone: 'violet' },
  { label: '阶段总数', value: stats.value.totalStages, icon: markRaw(Files), tone: 'green' },
  { label: '题目总数', value: stats.value.totalQuestions, icon: markRaw(Document), tone: 'orange' }
])

const subjectDialogVisible = ref(false)
const subjectDialogMode = ref<'create' | 'edit'>('create')
const editingSubjectId = ref<number | null>(null)
const subjectSubmitting = ref(false)
const subjectFormRef = ref<FormInstance>()
const subjectForm = reactive<SubjectSavePayload>({
  name: '',
  icon: '🤖',
  description: '',
  themeColor: themeColors[0],
  sortOrder: 1
})

const subjectRules: FormRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
    { max: 50, message: '学科名称不能超过 50 个字符', trigger: 'blur' }
  ],
  icon: [
    { required: true, message: '请输入学科图标', trigger: 'blur' },
    { max: 50, message: '学科图标不能超过 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入学科描述', trigger: 'blur' },
    { max: 255, message: '学科描述不能超过 255 个字符', trigger: 'blur' }
  ],
  themeColor: [{ required: true, message: '请选择主题颜色', trigger: 'change' }]
}

const stageDialogVisible = ref(false)
const stageSubjectId = ref<number | null>(null)
const editingStageId = ref<number | null>(null)
const stageSubmitting = ref(false)
const stageFormRef = ref<FormInstance>()
const stageForm = reactive({
  name: '',
  icon: '',
  sortOrder: 1
})

const stageRules: FormRules = {
  name: [
    { required: true, message: '请输入阶段名称', trigger: 'blur' },
    { max: 50, message: '阶段名称不能超过 50 个字符', trigger: 'blur' }
  ],
  icon: [{ max: 50, message: '阶段图标不能超过 50 个字符', trigger: 'blur' }]
}

onMounted(() => {
  void loadData().catch(() => undefined)
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const [statsData, subjectData] = await Promise.all([getSubjectStats(), listSubjects()])
    stats.value = statsData
    subjects.value = subjectData
  } finally {
    loading.value = false
  }
}

function openCreateSubject(): void {
  subjectDialogMode.value = 'create'
  editingSubjectId.value = null
  Object.assign(subjectForm, {
    name: '',
    icon: '🤖',
    description: '',
    themeColor: themeColors[0],
    sortOrder: subjects.value.length + 1
  })
  subjectDialogVisible.value = true
  nextTick(() => subjectFormRef.value?.clearValidate())
}

function openEditSubject(subject: SubjectCard): void {
  subjectDialogMode.value = 'edit'
  editingSubjectId.value = subject.id
  Object.assign(subjectForm, {
    name: subject.name,
    icon: subject.icon,
    description: subject.description,
    themeColor: subject.themeColor,
    sortOrder: subject.sortOrder
  })
  subjectDialogVisible.value = true
  nextTick(() => subjectFormRef.value?.clearValidate())
}

async function submitSubject(): Promise<void> {
  const formInstance = subjectFormRef.value
  if (!formInstance) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid) {
    return
  }
  subjectSubmitting.value = true
  try {
    const payload: SubjectSavePayload = {
      name: subjectForm.name.trim(),
      icon: subjectForm.icon.trim(),
      description: subjectForm.description.trim(),
      themeColor: subjectForm.themeColor,
      sortOrder: subjectForm.sortOrder
    }
    if (subjectDialogMode.value === 'create') {
      await createSubject(payload)
      ElMessage.success('学科创建成功')
    } else if (editingSubjectId.value) {
      await updateSubject(editingSubjectId.value, payload)
      ElMessage.success('学科信息已更新')
    }
    subjectDialogVisible.value = false
    await loadData()
  } finally {
    subjectSubmitting.value = false
  }
}

function openCreateStage(subject: SubjectCard): void {
  stageSubjectId.value = subject.id
  editingStageId.value = null
  Object.assign(stageForm, {
    name: '',
    icon: '',
    sortOrder: subject.stages.length + 1
  })
  stageDialogVisible.value = true
  nextTick(() => stageFormRef.value?.clearValidate())
}

function openEditStage(subject: SubjectCard, stage: StageItem): void {
  stageSubjectId.value = subject.id
  editingStageId.value = stage.id
  Object.assign(stageForm, {
    name: stage.name,
    icon: stage.icon ?? '',
    sortOrder: stage.sortOrder
  })
  stageDialogVisible.value = true
  nextTick(() => stageFormRef.value?.clearValidate())
}

async function submitStage(): Promise<void> {
  const formInstance = stageFormRef.value
  if (!formInstance || !stageSubjectId.value) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid) {
    return
  }
  stageSubmitting.value = true
  try {
    const payload: StageSavePayload = {
      name: stageForm.name.trim(),
      icon: stageForm.icon.trim() || undefined,
      sortOrder: stageForm.sortOrder
    }
    if (editingStageId.value) {
      await updateStage(stageSubjectId.value, editingStageId.value, payload)
      ElMessage.success('阶段信息已更新')
    } else {
      await createStage(stageSubjectId.value, payload)
      ElMessage.success('阶段创建成功')
    }
    stageDialogVisible.value = false
    await loadData()
  } finally {
    stageSubmitting.value = false
  }
}

async function moveSubject(index: number, offset: number): Promise<void> {
  const target = index + offset
  if (target < 0 || target >= subjects.value.length) {
    return
  }
  const ids = subjects.value.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await sortSubjects(ids)
  await loadData()
}

async function moveStage(subject: SubjectCard, index: number, offset: number): Promise<void> {
  const target = index + offset
  if (target < 0 || target >= subject.stages.length) {
    return
  }
  const ids = subject.stages.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await sortStages(subject.id, ids)
  await loadData()
}

async function handleStageStatus(
  subject: SubjectCard,
  stage: StageItem,
  value: string | number | boolean
): Promise<void> {
  const status = value ? 1 : 0
  try {
    await updateStageStatus(subject.id, stage.id, status)
    ElMessage.success(status === 1 ? '阶段已启用' : '阶段已停用')
  } finally {
    await loadData()
  }
}

async function confirmAction(message: string, title = '提示'): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    return true
  } catch {
    return false
  }
}

async function removeSubject(subject: SubjectCard): Promise<void> {
  const parts: string[] = []
  if (subject.stages.length > 0) {
    parts.push(`${subject.stages.length} 个阶段`)
  }
  if (subject.questionCount > 0) {
    parts.push(`${subject.questionCount} 道题目`)
  }
  if (subject.courseCount > 0) {
    parts.push(`${subject.courseCount} 门课程`)
  }
  if (subject.routeCount > 0) {
    parts.push(`${subject.routeCount} 条路线`)
  }
  if (parts.length > 0) {
    await ElMessageBox.alert(
      `学科「${subject.name}」下存在 ${parts.join('、')}，请先处理关联内容后再删除。`,
      '无法删除',
      { type: 'warning', confirmButtonText: '知道了' }
    )
    return
  }
  if (!(await confirmAction(`确定要删除学科「${subject.name}」吗？此操作不可恢复。`, '删除学科'))) {
    return
  }
  await deleteSubject(subject.id)
  ElMessage.success('学科已删除')
  await loadData()
}

async function removeStage(subject: SubjectCard, stage: StageItem): Promise<void> {
  if (stage.questionCount > 0) {
    await ElMessageBox.alert(
      `阶段「${stage.name}」下存在 ${stage.questionCount} 道题目，请先处理题目后再删除。`,
      '无法删除',
      { type: 'warning', confirmButtonText: '知道了' }
    )
    return
  }
  if (!(await confirmAction(`确定要删除阶段「${stage.name}」吗？`, '删除阶段'))) {
    return
  }
  await deleteStage(subject.id, stage.id)
  ElMessage.success('阶段已删除')
  await loadData()
}

function accentStyle(color: string) {
  return {
    background: `linear-gradient(135deg, ${color}, ${shadeColor(color, -20)})`
  }
}

function iconStyle(color: string) {
  return {
    background: hexToRgba(color, 0.12),
    color
  }
}

function hexToRgba(hex: string, alpha: number): string {
  const normalized = hex.replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(normalized)) {
    return `rgba(102, 126, 234, ${alpha})`
  }
  const r = Number.parseInt(normalized.slice(0, 2), 16)
  const g = Number.parseInt(normalized.slice(2, 4), 16)
  const b = Number.parseInt(normalized.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function shadeColor(hex: string, percent: number): string {
  const normalized = hex.replace('#', '')
  if (!/^[0-9a-fA-F]{6}$/.test(normalized)) {
    return '#5568d3'
  }
  const amount = Math.round(2.55 * percent)
  const channels = [0, 2, 4].map((start) => {
    const value = Number.parseInt(normalized.slice(start, start + 2), 16) + amount
    return Math.max(0, Math.min(255, value)).toString(16).padStart(2, '0')
  })
  return `#${channels.join('')}`
}
</script>

<template>
  <PageShell
    title="学科管理"
    description="维护学科与阶段：卡片统计课程/路线/题目数，支持排序、编辑、启停与删除保护。"
    :card="false"
  >
    <template #actions>
      <el-button v-permission="'subject:add'" type="primary" :icon="Plus" @click="openCreateSubject">
        新增学科
      </el-button>
    </template>

    <div v-loading="loading" class="subject-page">
      <div class="stats-grid">
        <div v-for="stat in statCards" :key="stat.label" class="stat-card">
          <div class="stat-icon" :class="`tone-${stat.tone}`">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-value">{{ stat.value.toLocaleString() }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <div class="subject-grid">
        <article v-for="(subject, index) in subjects" :key="subject.id" class="subject-card">
          <div class="subject-accent" :style="accentStyle(subject.themeColor)" />
          <header class="subject-header">
            <div class="subject-title">
              <span class="subject-icon" :style="iconStyle(subject.themeColor)">{{ subject.icon }}</span>
              <div class="subject-copy">
                <h3>
                  {{ subject.name }}
                  <span class="sort-badge">第 {{ index + 1 }} 位</span>
                </h3>
                <p>{{ subject.description }}</p>
              </div>
            </div>
            <div class="subject-actions">
              <el-tooltip content="上移">
                <el-button
                  v-permission="'subject:edit'"
                  text
                  circle
                  size="small"
                  :icon="Top"
                  :disabled="index === 0"
                  @click="moveSubject(index, -1)"
                />
              </el-tooltip>
              <el-tooltip content="下移">
                <el-button
                  v-permission="'subject:edit'"
                  text
                  circle
                  size="small"
                  :icon="Bottom"
                  :disabled="index === subjects.length - 1"
                  @click="moveSubject(index, 1)"
                />
              </el-tooltip>
              <el-tooltip content="编辑学科">
                <el-button
                  v-permission="'subject:edit'"
                  text
                  circle
                  size="small"
                  :icon="Edit"
                  @click="openEditSubject(subject)"
                />
              </el-tooltip>
              <el-tooltip content="删除学科">
                <el-button
                  v-permission="'subject:delete'"
                  text
                  circle
                  size="small"
                  type="danger"
                  :icon="Delete"
                  @click="removeSubject(subject)"
                />
              </el-tooltip>
            </div>
          </header>

          <div class="subject-stats">
            <span><el-icon><VideoCamera /></el-icon>课程 <strong>{{ subject.courseCount }}</strong></span>
            <span><el-icon><Guide /></el-icon>路线 <strong>{{ subject.routeCount }}</strong></span>
            <span><el-icon><Document /></el-icon>题目 <strong>{{ subject.questionCount }}</strong></span>
          </div>

          <div class="stage-section">
            <div class="stage-head">
              <span>阶段</span>
              <span class="stage-count">{{ subject.stages.length }}</span>
            </div>
            <div v-if="!subject.stages.length" class="stage-empty">暂无阶段，点击下方按钮添加</div>
            <div
              v-for="(stage, stageIndex) in subject.stages"
              :key="stage.id"
              class="stage-row"
            >
              <span class="stage-icon">{{ stage.icon || '📁' }}</span>
              <div class="stage-info">
                <div class="stage-name">
                  {{ stage.name }}
                  <span class="sort-chip">{{ stage.sortOrder }}</span>
                </div>
                <div class="stage-meta">{{ stage.questionCount }} 道题目</div>
              </div>
              <div class="stage-actions">
                <el-button
                  v-permission="'subject:edit'"
                  text
                  circle
                  size="small"
                  :icon="Top"
                  :disabled="stageIndex === 0"
                  @click="moveStage(subject, stageIndex, -1)"
                />
                <el-button
                  v-permission="'subject:edit'"
                  text
                  circle
                  size="small"
                  :icon="Bottom"
                  :disabled="stageIndex === subject.stages.length - 1"
                  @click="moveStage(subject, stageIndex, 1)"
                />
                <el-switch
                  v-permission="'subject:edit'"
                  :model-value="stage.status === 1"
                  size="small"
                  @change="handleStageStatus(subject, stage, $event)"
                />
                <el-button
                  v-permission="'subject:edit'"
                  text
                  circle
                  size="small"
                  :icon="Edit"
                  @click="openEditStage(subject, stage)"
                />
                <el-button
                  v-permission="'subject:delete'"
                  text
                  circle
                  size="small"
                  type="danger"
                  :icon="Delete"
                  @click="removeStage(subject, stage)"
                />
              </div>
            </div>
            <el-button
              v-permission="'subject:add'"
              text
              type="primary"
              class="add-stage-btn"
              :icon="Plus"
              @click="openCreateStage(subject)"
            >
              添加阶段
            </el-button>
          </div>
        </article>

        <button
          v-permission="'subject:add'"
          type="button"
          class="add-subject-card"
          @click="openCreateSubject"
        >
          <el-icon><Plus /></el-icon>
          <strong>新增学科</strong>
          <span>点击创建新学科</span>
        </button>
      </div>
    </div>

    <el-dialog
      v-model="subjectDialogVisible"
      :title="subjectDialogMode === 'create' ? '新增学科' : '编辑学科'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="subjectFormRef" :model="subjectForm" :rules="subjectRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="学科名称" prop="name">
            <el-input v-model="subjectForm.name" maxlength="50" placeholder="请输入学科名称" />
          </el-form-item>
          <el-form-item label="学科图标（Emoji）" prop="icon">
            <el-input v-model="subjectForm.icon" maxlength="8" placeholder="例如：🤖">
              <template #prepend>
                <span class="emoji-preview">{{ subjectForm.icon || '📘' }}</span>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="排序号" prop="sortOrder">
            <el-input-number
              v-model="subjectForm.sortOrder"
              :min="0"
              :max="999"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="主题颜色" prop="themeColor">
            <div class="color-swatches">
              <button
                v-for="color in themeColors"
                :key="color"
                type="button"
                class="color-swatch"
                :class="{ 'is-active': subjectForm.themeColor === color }"
                :style="{ background: color }"
                :aria-label="color"
                @click="subjectForm.themeColor = color"
              />
            </div>
          </el-form-item>
          <el-form-item label="学科描述" prop="description" class="span-2">
            <el-input
              v-model="subjectForm.description"
              type="textarea"
              :rows="3"
              maxlength="255"
              show-word-limit
              placeholder="请输入学科描述"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="subjectDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="subjectSubmitting" @click="submitSubject">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="stageDialogVisible"
      :title="editingStageId ? '编辑阶段' : '新增阶段'"
      width="480px"
      align-center
      destroy-on-close
    >
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-position="top">
        <el-form-item label="阶段名称" prop="name">
          <el-input v-model="stageForm.name" maxlength="50" placeholder="请输入阶段名称" />
        </el-form-item>
        <el-form-item label="阶段图标" prop="icon">
          <el-input v-model="stageForm.icon" maxlength="8" placeholder="选填，例如：🚀">
            <template #suffix>
              <span class="stage-icon-preview">{{ stageForm.icon || '📁' }}</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number
            v-model="stageForm.sortOrder"
            :min="0"
            :max="999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="stageSubmitting" @click="submitStage">
          确定
        </el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.subject-page {
  min-height: 320px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 21px;
}

.tone-blue {
  color: #409eff;
  background: rgba(64, 158, 255, 0.12);
}

.tone-violet {
  color: #7c6cf0;
  background: rgba(124, 108, 240, 0.12);
}

.tone-green {
  color: #22b07d;
  background: rgba(34, 176, 125, 0.12);
}

.tone-orange {
  color: #f5a623;
  background: rgba(245, 166, 35, 0.14);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--ys-text-primary);
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.subject-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.subject-accent {
  height: 4px;
}

.subject-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 14px;
}

.subject-title {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.subject-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 22px;
}

.subject-copy {
  min-width: 0;
}

.subject-copy h3 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.subject-copy p {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ys-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sort-badge {
  padding: 2px 7px;
  border-radius: 999px;
  background: var(--ys-bg);
  color: var(--ys-text-secondary);
  font-size: 11px;
  font-weight: 500;
}

.subject-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: none;
}

.subject-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 20px;
  background: #f8f9fc;
  border-top: 1px solid var(--ys-border-light);
  border-bottom: 1px solid var(--ys-border-light);
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.subject-stats span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.subject-stats strong {
  color: var(--ys-text-primary);
  font-weight: 600;
}

.stage-section {
  padding: 14px 20px 18px;
}

.stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ys-text-secondary);
}

.stage-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--ys-bg);
  font-weight: 500;
}

.stage-empty {
  padding: 12px 0;
  font-size: 12px;
  color: var(--ys-text-placeholder);
}

.stage-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: var(--ys-radius-sm);
  transition: background 0.2s ease;
}

.stage-row:hover {
  background: var(--ys-bg);
}

.stage-icon {
  flex: none;
  width: 24px;
  text-align: center;
  font-size: 16px;
}

.stage-info {
  flex: 1;
  min-width: 0;
}

.stage-name {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--ys-text-primary);
}

.sort-chip {
  flex: none;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--ys-bg);
  color: var(--ys-text-secondary);
  font-size: 10px;
}

.stage-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.stage-actions {
  display: flex;
  align-items: center;
  gap: 1px;
  flex: none;
}

.add-stage-btn {
  margin-top: 6px;
}

.add-subject-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 220px;
  padding: 24px;
  border: 1px dashed var(--ys-border);
  border-radius: var(--ys-radius-lg);
  background: rgba(255, 255, 255, 0.7);
  color: var(--ys-text-secondary);
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.add-subject-card:hover {
  border-color: var(--ys-primary);
  color: var(--ys-primary);
  transform: translateY(-2px);
}

.add-subject-card .el-icon {
  font-size: 28px;
}

.add-subject-card strong {
  font-size: 15px;
  font-weight: 600;
}

.add-subject-card span {
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.emoji-preview {
  font-size: 18px;
}

.stage-icon-preview {
  font-size: 16px;
  line-height: 1;
}

.color-swatches {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
}

.color-swatch {
  width: 24px;
  height: 24px;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.color-swatch:hover {
  transform: scale(1.08);
}

.color-swatch.is-active {
  border-color: #fff;
  box-shadow: 0 0 0 2px var(--ys-primary);
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .subject-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }

  .subject-stats {
    gap: 12px;
    flex-wrap: wrap;
  }
}
</style>
