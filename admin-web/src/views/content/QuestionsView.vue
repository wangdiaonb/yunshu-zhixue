<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions
} from 'element-plus'
import {
  Bottom,
  Check,
  CircleCheck,
  CircleClose,
  Delete,
  Download,
  Edit,
  Plus,
  Refresh,
  RefreshLeft,
  Search,
  Top,
  Upload,
  View
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  auditQuestion,
  batchDeleteQuestions,
  batchQuestionStatus,
  changeQuestionStatus,
  confirmQuestionImport,
  createQuestion,
  deleteQuestion,
  downloadQuestionTemplate,
  exportQuestions,
  getQuestionDetail,
  getQuestionStats,
  pageQuestions,
  previewQuestionImport,
  resubmitQuestion,
  updateQuestion
} from '@/api/question'
import { listSubjectOptions, listSubjects } from '@/api/subject'
import type {
  QuestionDetail,
  QuestionImportPreview,
  QuestionItem,
  QuestionOptionPayload,
  QuestionSavePayload,
  QuestionStats
} from '@/types/question'
import type { SubjectCard, SubjectOption } from '@/types/subject'

interface StageOption {
  id: number
  name: string
  subjectId: number
  subjectName: string
}

interface OptionForm {
  key: string
  text: string
  correct: boolean
}

const stats = ref<QuestionStats>({
  total: 0,
  published: 0,
  pending: 0,
  single: 0,
  multiple: 0,
  judge: 0,
  shortAnswer: 0
})
const questions = ref<QuestionItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const selectedRows = ref<QuestionItem[]>([])
const subjectCards = ref<SubjectCard[]>([])
const subjectOptions = ref<SubjectOption[]>([])
const stageOptions = ref<StageOption[]>([])

const filters = reactive<{
  keyword: string
  subjectId?: number
  stageId?: number
  type?: number
  difficulty?: number
  status?: number
}>({
  keyword: '',
  subjectId: undefined,
  stageId: undefined,
  type: undefined,
  difficulty: undefined,
  status: undefined
})

const filteredStages = computed(() => {
  if (!filters.subjectId) {
    return stageOptions.value
  }
  return stageOptions.value.filter((stage) => stage.subjectId === filters.subjectId)
})

const statCards = computed(() => [
  { label: '题目总数', value: stats.value.total, tone: 'blue' },
  { label: '已上架', value: stats.value.published, tone: 'green' },
  { label: '待审核', value: stats.value.pending, tone: 'orange' },
  { label: '单选题', value: stats.value.single, tone: 'primary' },
  { label: '多选题', value: stats.value.multiple, tone: 'violet' },
  { label: '判断题', value: stats.value.judge, tone: 'cyan' },
  { label: '简答题', value: stats.value.shortAnswer, tone: 'pink' }
])

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const formSubmitting = ref(false)
const form = reactive<{
  content: string
  subjectId: number
  stageId: number
  type: number
  difficulty: number
  tags: string
  analysis: string
  answer: string
  options: OptionForm[]
}>({
  content: '',
  subjectId: 0,
  stageId: 0,
  type: 1,
  difficulty: 2,
  tags: '',
  analysis: '',
  answer: '',
  options: []
})

const formStages = computed(() =>
  stageOptions.value.filter((stage) => stage.subjectId === form.subjectId)
)

const formRules: FormRules = {
  content: [
    { required: true, message: '请输入题干', trigger: 'blur' },
    { max: 10000, message: '题干过长', trigger: 'blur' }
  ],
  stageId: [
    {
      validator: (_rule, value: number, callback) => {
        if (!value) {
          callback(new Error('请选择阶段'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  tags: [{ max: 255, message: '标签不能超过 255 个字符', trigger: 'blur' }]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<QuestionDetail | null>(null)

const importVisible = ref(false)
const importLoading = ref(false)
const importConfirming = ref(false)
const importFileName = ref('')
const importSubjectId = ref<number | undefined>()
const importPreview = ref<QuestionImportPreview | null>(null)

const exporting = ref(false)

let keywordTimer: ReturnType<typeof setTimeout> | undefined

onMounted(() => {
  void loadAll().catch(() => undefined)
})

onUnmounted(() => {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
})

async function loadAll(): Promise<void> {
  await Promise.all([loadSubjects(), loadStats(), loadQuestions()])
}

async function loadSubjects(): Promise<void> {
  const [cards, options] = await Promise.all([listSubjects(), listSubjectOptions()])
  subjectCards.value = cards
  subjectOptions.value = options
  stageOptions.value = cards.flatMap((subject) =>
    subject.stages.map((stage) => ({
      id: stage.id,
      name: stage.name,
      subjectId: subject.id,
      subjectName: subject.name
    }))
  )
}

async function loadStats(): Promise<void> {
  stats.value = await getQuestionStats()
}

async function loadQuestions(): Promise<void> {
  loading.value = true
  try {
    const result = await pageQuestions({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      subjectId: filters.subjectId,
      stageId: filters.stageId,
      type: filters.type,
      difficulty: filters.difficulty,
      status: filters.status
    })
    questions.value = result.list
    total.value = result.total
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

function handleKeywordInput(): void {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
  keywordTimer = setTimeout(() => {
    page.value = 1
    void loadQuestions().catch(() => undefined)
  }, 300)
}

function handleSearch(): void {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
  page.value = 1
  void loadQuestions().catch(() => undefined)
}

function resetFilters(): void {
  filters.keyword = ''
  filters.subjectId = undefined
  filters.stageId = undefined
  filters.type = undefined
  filters.difficulty = undefined
  filters.status = undefined
  page.value = 1
  void loadQuestions().catch(() => undefined)
}

function handleSubjectFilterChange(): void {
  filters.stageId = undefined
}

function handleSizeChange(): void {
  page.value = 1
  void loadQuestions().catch(() => undefined)
}

function handleSelectionChange(rows: QuestionItem[]): void {
  selectedRows.value = rows
}

async function refreshAfterChange(): Promise<void> {
  await Promise.all([loadStats(), loadQuestions()])
  if (detailVisible.value && detail.value) {
    detail.value = await getQuestionDetail(detail.value.id)
  }
}

function firstStageForSubject(subjectId: number): number {
  return stageOptions.value.find((stage) => stage.subjectId === subjectId)?.id ?? 0
}

function defaultSubjectId(): number {
  return (
    subjectCards.value.find((subject) => subject.stages.length > 0)?.id ??
    subjectOptions.value[0]?.id ??
    0
  )
}

function initializeOptions(type: number): OptionForm[] {
  const options = ['A', 'B', 'C', 'D'].map((key) => ({
    key,
    text: '',
    correct: false
  }))
  if (type === 1 && options[0]) {
    options[0].correct = true
  }
  return options
}

function openCreate(): void {
  const subjectId = defaultSubjectId()
  formMode.value = 'create'
  editingId.value = null
  Object.assign(form, {
    content: '',
    subjectId,
    stageId: firstStageForSubject(subjectId),
    type: 1,
    difficulty: 2,
    tags: '',
    analysis: '',
    answer: '',
    options: initializeOptions(1)
  })
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function openEdit(row: QuestionItem): Promise<void> {
  const data = await getQuestionDetail(row.id)
  const stage = stageOptions.value.find((item) => item.id === data.stageId)
  formMode.value = 'edit'
  editingId.value = data.id
  Object.assign(form, {
    content: data.content,
    subjectId: stage?.subjectId ?? defaultSubjectId(),
    stageId: data.stageId,
    type: data.type,
    difficulty: data.difficulty,
    tags: data.tags ?? '',
    analysis: data.analysis ?? '',
    answer: data.answer ?? '',
    options:
      data.type === 1 || data.type === 2
        ? data.options.map((option) => ({
            key: option.key,
            text: option.text,
            correct: option.correct
          }))
        : []
  })
  if ((data.type === 1 || data.type === 2) && form.options.length < 2) {
    form.options = initializeOptions(data.type)
  }
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleFormSubjectChange(): void {
  form.stageId = firstStageForSubject(form.subjectId)
}

function handleTypeChange(type: number): void {
  if (type === 1 || type === 2) {
    if (form.options.length < 2) {
      form.options = initializeOptions(type)
    } else if (type === 1) {
      let found = false
      form.options.forEach((option) => {
        if (option.correct && !found) {
          found = true
        } else {
          option.correct = false
        }
      })
      if (!found && form.options[0]) {
        form.options[0].correct = true
      }
    }
    form.answer = ''
  } else {
    form.options = []
    form.answer = type === 3 ? '正确' : ''
  }
}

function addOption(): void {
  if (form.options.length >= 6) {
    return
  }
  form.options.push({
    key: String.fromCharCode(65 + form.options.length),
    text: '',
    correct: false
  })
}

function removeOption(index: number): void {
  if (form.options.length <= 2) {
    return
  }
  form.options.splice(index, 1)
  form.options.forEach((option, optionIndex) => {
    option.key = String.fromCharCode(65 + optionIndex)
  })
}

function setSingleCorrect(index: number): void {
  form.options.forEach((option, optionIndex) => {
    option.correct = optionIndex === index
  })
}

function validateOptions(): boolean {
  if (form.type === 1 || form.type === 2) {
    if (form.options.length < 2 || form.options.length > 6) {
      ElMessage.warning('单选/多选题需设置 2~6 个选项')
      return false
    }
    if (form.options.some((option) => !option.text.trim())) {
      ElMessage.warning('请填写完整的选项内容')
      return false
    }
    const correctCount = form.options.filter((option) => option.correct).length
    if (form.type === 1 && correctCount !== 1) {
      ElMessage.warning('单选题必须且只能有 1 个正确选项')
      return false
    }
    if (form.type === 2 && correctCount < 1) {
      ElMessage.warning('多选题至少需要 1 个正确选项')
      return false
    }
  } else if (form.type === 3) {
    if (!['正确', '错误'].includes(form.answer)) {
      ElMessage.warning('请选择判断题答案')
      return false
    }
  } else if (!form.answer.trim()) {
    ElMessage.warning('请填写简答题参考答案')
    return false
  }
  return true
}

async function submitForm(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid || !validateOptions()) {
    return
  }
  formSubmitting.value = true
  try {
    const options: QuestionOptionPayload[] | undefined =
      form.type === 1 || form.type === 2
        ? form.options.map((option) => ({
            text: option.text.trim(),
            correct: option.correct
          }))
        : undefined
    const payload: QuestionSavePayload = {
      content: form.content.trim(),
      stageId: form.stageId,
      type: form.type,
      difficulty: form.difficulty,
      tags: form.tags.trim() || undefined,
      analysis: form.analysis.trim() || undefined,
      answer:
        form.type === 3 || form.type === 4 ? form.answer.trim() || undefined : undefined,
      options
    }
    if (formMode.value === 'create') {
      await createQuestion(payload)
      ElMessage.success('题目已创建，状态为待审核')
    } else if (editingId.value) {
      await updateQuestion(editingId.value, payload)
      ElMessage.success('题目已更新')
    }
    formVisible.value = false
    await refreshAfterChange()
  } finally {
    formSubmitting.value = false
  }
}

async function openDetail(row: QuestionItem): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getQuestionDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function handlePass(row: QuestionItem): Promise<void> {
  try {
    await ElMessageBox.confirm('审核通过后题目将立即上架，确定继续吗？', '审核通过', {
      type: 'warning',
      confirmButtonText: '通过',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await auditQuestion(row.id, { pass: true })
  ElMessage.success('审核通过，题目已上架')
  await refreshAfterChange()
}

async function handleReject(row: QuestionItem): Promise<void> {
  try {
    const result = await ElMessageBox.prompt('请输入驳回原因', '审核不通过', {
      inputType: 'textarea',
      inputPlaceholder: '请填写驳回原因（必填）',
      inputValidator: (value) =>
        value && value.trim() ? true : '请输入驳回原因',
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消'
    })
    await auditQuestion(row.id, { pass: false, rejectReason: result.value.trim() })
    ElMessage.success('题目已驳回')
    await refreshAfterChange()
  } catch {
    // 取消操作
  }
}

async function handleStatus(row: QuestionItem, action: 'on' | 'off'): Promise<void> {
  const label = action === 'on' ? '重新上架' : '下架'
  try {
    await ElMessageBox.confirm(
      action === 'on'
        ? '重新上架后题目将进入待审核队列，确定继续吗？'
        : '确定要下架该题目吗？',
      label,
      { type: 'warning', confirmButtonText: label, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await changeQuestionStatus(row.id, action)
  ElMessage.success(action === 'on' ? '已提交审核' : '题目已下架')
  await refreshAfterChange()
}

async function handleResubmit(row: QuestionItem): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要重新提交该题目进入审核队列吗？', '重新提交', {
      type: 'warning',
      confirmButtonText: '重新提交',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await resubmitQuestion(row.id)
  ElMessage.success('已重新提交审核')
  await refreshAfterChange()
}

async function handleDelete(row: QuestionItem): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后学员端不再展示该题目，确定继续吗？', '删除题目', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const result = await deleteQuestion(row.id)
  ElMessage.success(
    result.refCount > 0
      ? `题目已删除，存在 ${result.refCount} 条练习/错题记录`
      : '题目已删除'
  )
  if (detail.value?.id === row.id) {
    detailVisible.value = false
  }
  await refreshAfterChange()
}

async function handleBatchStatus(action: 'on' | 'off'): Promise<void> {
  if (selectedRows.value.length === 0) {
    return
  }
  const label = action === 'on' ? '批量上架' : '批量下架'
  try {
    await ElMessageBox.confirm(
      action === 'on'
        ? `将选中的 ${selectedRows.value.length} 道题提交重新上架审核？`
        : `确定要下架选中的 ${selectedRows.value.length} 道题吗？`,
      label,
      { type: 'warning', confirmButtonText: label, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await batchQuestionStatus(
    selectedRows.value.map((row) => row.id),
    action
  )
  ElMessage.success(
    `已处理 ${result.updatedCount} 道题${result.skippedCount ? `，跳过 ${result.skippedCount} 道` : ''}`
  )
  await refreshAfterChange()
}

async function handleBatchDelete(): Promise<void> {
  if (selectedRows.value.length === 0) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 道题吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await batchDeleteQuestions(selectedRows.value.map((row) => row.id))
  ElMessage.success(
    `已删除 ${result.deletedCount} 道题${result.refCount ? `，存在 ${result.refCount} 条关联记录` : ''}`
  )
  await refreshAfterChange()
}

function openImport(): void {
  resetImport()
  importSubjectId.value = subjectOptions.value[0]?.id
  importVisible.value = true
}

function resetImport(): void {
  importLoading.value = false
  importConfirming.value = false
  importFileName.value = ''
  importPreview.value = null
}

function handleImportSubjectChange(): void {
  importPreview.value = null
  importFileName.value = ''
}

async function handleTemplateDownload(): Promise<void> {
  const blob = await downloadQuestionTemplate()
  downloadBlob(blob, '题库导入模板.xlsx')
}

async function handleImportRequest(options: UploadRequestOptions): Promise<void> {
  if (!importSubjectId.value) {
    ElMessage.warning('请先选择目标学科')
    return
  }
  const file = options.file as File
  importFileName.value = file.name
  importLoading.value = true
  importPreview.value = null
  try {
    const preview = await previewQuestionImport(file, importSubjectId.value)
    importPreview.value = preview
    options.onSuccess?.(preview)
    if (preview.validCount > 0) {
      ElMessage.success(`解析完成，有效 ${preview.validCount} 道`)
    } else {
      ElMessage.warning('没有可导入的有效题目')
    }
  } catch {
    // 错误提示由统一请求层展示
  } finally {
    importLoading.value = false
  }
}

async function handleConfirmImport(): Promise<void> {
  const token = importPreview.value?.token
  if (!token) {
    return
  }
  importConfirming.value = true
  try {
    const result = await confirmQuestionImport(token)
    ElMessage.success(
      `成功导入 ${result.imported} 道题${result.stagesCreated ? `，自动创建 ${result.stagesCreated} 个阶段` : ''}`
    )
    importVisible.value = false
    await loadAll()
  } finally {
    importConfirming.value = false
  }
}

async function handleExport(): Promise<void> {
  exporting.value = true
  try {
    const blob = await exportQuestions({
      keyword: filters.keyword.trim() || undefined,
      subjectId: filters.subjectId,
      stageId: filters.stageId,
      type: filters.type,
      difficulty: filters.difficulty,
      status: filters.status
    })
    downloadBlob(blob, '题库导出.xlsx')
  } finally {
    exporting.value = false
  }
}

function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function typeLabel(type: number): string {
  return { 1: '单选', 2: '多选', 3: '判断', 4: '简答' }[type] ?? '-'
}

function typeTagType(type: number): 'primary' | 'warning' | 'success' | 'info' {
  return ({ 1: 'primary', 2: 'warning', 3: 'success', 4: 'info' } as const)[type] ?? 'info'
}

function difficultyLabel(difficulty: number): string {
  return { 1: '简单', 2: '中等', 3: '困难' }[difficulty] ?? '-'
}

function difficultyTagType(difficulty: number): 'success' | 'warning' | 'danger' {
  return ({ 1: 'success', 2: 'warning', 3: 'danger' } as const)[difficulty] ?? 'warning'
}

function statusLabel(status: number): string {
  return { 1: '已上架', 2: '已下架', 3: '待审核', 5: '已驳回' }[status] ?? '草稿'
}

function statusTagType(status: number): 'success' | 'info' | 'warning' | 'danger' {
  return ({ 1: 'success', 2: 'info', 3: 'warning', 5: 'danger' } as const)[status] ?? 'info'
}

function formatDateTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<template>
  <PageShell
    title="题库管理"
    description="维护题目、选项与解析，支持 Excel 批量导入导出、审核与上下架状态流转。"
    :card="false"
  >
    <template #actions>
      <el-button v-permission="'question:import'" :icon="Upload" @click="openImport">
        导入 Excel
      </el-button>
      <el-button
        v-permission="'question:export'"
        :icon="Download"
        :loading="exporting"
        @click="handleExport"
      >
        导出 Excel
      </el-button>
      <el-button v-permission="'question:add'" type="primary" :icon="Plus" @click="openCreate">
        新增题目
      </el-button>
    </template>

    <div class="question-page">
      <div class="stats-grid">
        <div v-for="stat in statCards" :key="stat.label" class="stat-card" :class="`tone-${stat.tone}`">
          <div class="stat-value">{{ stat.value.toLocaleString() }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>

      <div class="table-card">
        <div class="filter-bar">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索题干 / 标签"
            clearable
            style="width: 240px"
            :prefix-icon="Search"
            @input="handleKeywordInput"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="filters.subjectId"
            placeholder="全部学科"
            clearable
            style="width: 175px"
            @change="handleSubjectFilterChange"
          >
            <el-option
              v-for="subject in subjectOptions"
              :key="subject.id"
              :label="`${subject.icon} ${subject.name}`"
              :value="subject.id"
            />
          </el-select>
          <el-select v-model="filters.stageId" placeholder="全部阶段" clearable style="width: 175px">
            <el-option
              v-for="stage in filteredStages"
              :key="stage.id"
              :label="`${stage.name} · ${stage.subjectName}`"
              :value="stage.id"
            />
          </el-select>
          <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 120px">
            <el-option label="单选" :value="1" />
            <el-option label="多选" :value="2" />
            <el-option label="判断" :value="3" />
            <el-option label="简答" :value="4" />
          </el-select>
          <el-select v-model="filters.difficulty" placeholder="全部难度" clearable style="width: 120px">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="已上架" :value="1" />
            <el-option label="已下架" :value="2" />
            <el-option label="待审核" :value="3" />
            <el-option label="已驳回" :value="5" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
        </div>

        <div v-if="selectedRows.length" class="batch-bar">
          <span>已选择 <strong>{{ selectedRows.length }}</strong> 道题</span>
          <el-button
            v-permission="'question:edit'"
            size="small"
            :icon="Top"
            @click="handleBatchStatus('on')"
          >
            批量上架
          </el-button>
          <el-button
            v-permission="'question:edit'"
            size="small"
            :icon="Bottom"
            @click="handleBatchStatus('off')"
          >
            批量下架
          </el-button>
          <el-button
            v-permission="'question:delete'"
            size="small"
            type="danger"
            plain
            :icon="Delete"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="questions"
          row-key="id"
          empty-text="暂无匹配的题目，试试调整筛选条件"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="44" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column label="题目" min-width="240">
            <template #default="{ row }">
              <div class="question-content" @click="openDetail(row)">{{ row.content }}</div>
              <div v-if="row.tags" class="question-tags">{{ row.tags }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="stageName" label="阶段" width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="75" align="center">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.type)" size="small" effect="plain">
                {{ typeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="75" align="center">
            <template #default="{ row }">
              <el-tag :type="difficultyTagType(row.difficulty)" size="small" effect="plain">
                {{ difficultyLabel(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="85" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="120">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="350"
            fixed="right"
            class-name="question-actions-column"
            label-class-name="question-actions-column"
          >
            <template #default="{ row }">
              <div class="row-actions">
                <el-button link type="primary" size="small" :icon="View" @click="openDetail(row)">
                  详情
                </el-button>
                <template v-if="row.status === 3">
                  <el-button
                    v-permission="'question:audit'"
                    link
                    type="success"
                    size="small"
                    :icon="CircleCheck"
                    @click="handlePass(row)"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-permission="'question:audit'"
                    link
                    type="danger"
                    size="small"
                    :icon="CircleClose"
                    @click="handleReject(row)"
                  >
                    不通过
                  </el-button>
                </template>
                <el-button
                  v-if="row.status === 2"
                  v-permission="'question:edit'"
                  link
                  type="success"
                  size="small"
                  :icon="Top"
                  @click="handleStatus(row, 'on')"
                >
                  上架
                </el-button>
                <el-button
                  v-if="row.status === 1"
                  v-permission="'question:edit'"
                  link
                  type="warning"
                  size="small"
                  :icon="Bottom"
                  @click="handleStatus(row, 'off')"
                >
                  下架
                </el-button>
                <el-button
                  v-if="row.status === 5"
                  v-permission="'question:edit'"
                  link
                  type="primary"
                  size="small"
                  :icon="Refresh"
                  @click="handleResubmit(row)"
                >
                  重提
                </el-button>
                <el-button
                  v-permission="'question:edit'"
                  link
                  type="primary"
                  size="small"
                  :icon="Edit"
                  @click="openEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-permission="'question:delete'"
                  link
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadQuestions"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增题目' : '编辑题目'"
      width="780px"
      align-center
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="题干" prop="content" class="span-2">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              maxlength="10000"
              show-word-limit
              placeholder="请输入题干"
            />
          </el-form-item>
          <el-form-item label="所属学科">
            <el-select v-model="form.subjectId" style="width: 100%" @change="handleFormSubjectChange">
              <el-option
                v-for="subject in subjectOptions"
                :key="subject.id"
                :label="`${subject.icon} ${subject.name}`"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="阶段" prop="stageId">
            <el-select v-model="form.stageId" style="width: 100%" placeholder="请选择阶段">
              <el-option
                v-for="stage in formStages"
                :key="stage.id"
                :label="stage.name"
                :value="stage.id"
              />
            </el-select>
            <div v-if="!formStages.length" class="hint">
              该学科暂无阶段，请先在学科管理中创建阶段
            </div>
          </el-form-item>
          <el-form-item label="题型">
            <el-radio-group v-model="form.type" @change="handleTypeChange">
              <el-radio :value="1">单选</el-radio>
              <el-radio :value="2">多选</el-radio>
              <el-radio :value="3">判断</el-radio>
              <el-radio :value="4">简答</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="难度">
            <el-radio-group v-model="form.difficulty">
              <el-radio :value="1">简单</el-radio>
              <el-radio :value="2">中等</el-radio>
              <el-radio :value="3">困难</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="标签" prop="tags">
            <el-input v-model="form.tags" maxlength="255" placeholder="多个标签用逗号分隔" />
          </el-form-item>
          <el-form-item label="状态">
            <el-tag type="warning" effect="plain">
              {{ formMode === 'create' ? '新增后待审核' : '保存后保持原状态' }}
            </el-tag>
          </el-form-item>

          <el-form-item
            v-if="form.type === 1 || form.type === 2"
            label="选项管理"
            class="span-2"
          >
            <div class="option-editor">
              <div v-for="(option, index) in form.options" :key="option.key" class="option-row">
                <span class="option-key">{{ option.key }}</span>
                <el-input
                  v-model="option.text"
                  maxlength="500"
                  :placeholder="`选项 ${option.key} 内容`"
                />
                <label class="correct-control">
                  <input
                    v-if="form.type === 1"
                    type="radio"
                    name="correct-option"
                    :checked="option.correct"
                    @change="setSingleCorrect(index)"
                  />
                  <el-checkbox v-else v-model="option.correct">正确</el-checkbox>
                  <span v-if="form.type === 1">正确</span>
                </label>
                <el-button
                  text
                  circle
                  size="small"
                  type="danger"
                  :icon="Delete"
                  :disabled="form.options.length <= 2"
                  @click="removeOption(index)"
                />
              </div>
              <el-button
                text
                type="primary"
                :icon="Plus"
                :disabled="form.options.length >= 6"
                @click="addOption"
              >
                添加选项
              </el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="form.type === 3" label="正确答案" class="span-2">
            <el-radio-group v-model="form.answer">
              <el-radio value="正确">正确</el-radio>
              <el-radio value="错误">错误</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="form.type === 4" label="参考答案" class="span-2">
            <el-input
              v-model="form.answer"
              type="textarea"
              :rows="4"
              maxlength="10000"
              placeholder="请输入简答题参考答案"
            />
          </el-form-item>

          <el-form-item label="解析" class="span-2">
            <el-input
              v-model="form.analysis"
              type="textarea"
              :rows="3"
              maxlength="10000"
              placeholder="请输入题目解析（可选）"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="formSubmitting" @click="submitForm">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailVisible"
      title="题目详情"
      width="760px"
      align-center
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-meta">
            <el-tag effect="plain">{{ detail.subjectName }} / {{ detail.stageName }}</el-tag>
            <el-tag :type="typeTagType(detail.type)" effect="plain">{{ typeLabel(detail.type) }}</el-tag>
            <el-tag :type="difficultyTagType(detail.difficulty)" effect="plain">
              {{ difficultyLabel(detail.difficulty) }}
            </el-tag>
            <el-tag :type="statusTagType(detail.status)" effect="plain">
              {{ statusLabel(detail.status) }}
            </el-tag>
            <span v-if="detail.tags" class="detail-tags">{{ detail.tags }}</span>
          </div>
          <div class="detail-content">{{ detail.content }}</div>

          <div v-if="detail.type === 1 || detail.type === 2" class="detail-options">
            <div
              v-for="option in detail.options"
              :key="option.id"
              class="detail-option"
              :class="{ 'is-correct': option.correct }"
            >
              <span class="option-key">{{ option.key }}</span>
              <span>{{ option.text }}</span>
              <el-tag v-if="option.correct" type="success" size="small" effect="plain">正确</el-tag>
            </div>
          </div>
          <div v-if="detail.type === 3 || detail.type === 4" class="detail-block">
            <strong>{{ detail.type === 3 ? '正确答案' : '参考答案' }}</strong>
            <p>{{ detail.answer || '未填写' }}</p>
          </div>
          <div v-if="detail.analysis" class="detail-block">
            <strong>解析</strong>
            <p>{{ detail.analysis }}</p>
          </div>
          <div v-if="detail.rejectReason" class="detail-block reject-reason">
            <strong>驳回原因</strong>
            <p>{{ detail.rejectReason }}</p>
          </div>
          <div class="detail-section-title">
            <span class="detail-section-bar" />
            答题统计
          </div>
          <div class="detail-stat-grid">
            <div class="detail-stat-card detail-stat-card--blue">
              <strong>{{ detail.doneCount }}</strong>
              <span>答题次数</span>
            </div>
            <div class="detail-stat-card detail-stat-card--green">
              <strong>{{ detail.correctCount }}</strong>
              <span>正确次数</span>
            </div>
            <div class="detail-stat-card detail-stat-card--orange">
              <strong>{{ detail.accuracyRate }}%</strong>
              <span>正确率</span>
            </div>
          </div>
          <div class="detail-created">创建时间 {{ formatDateTime(detail.createdAt) }}</div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="detail">
          <template v-if="detail.status === 3">
            <el-button
              v-permission="'question:audit'"
              type="success"
              :icon="CircleCheck"
              @click="handlePass(detail)"
            >
              审核通过
            </el-button>
            <el-button
              v-permission="'question:audit'"
              type="danger"
              plain
              :icon="CircleClose"
              @click="handleReject(detail)"
            >
              审核不通过
            </el-button>
          </template>
          <el-button
            v-if="detail.status === 1"
            v-permission="'question:edit'"
            type="warning"
            plain
            :icon="Bottom"
            @click="handleStatus(detail, 'off')"
          >
            下架
          </el-button>
          <el-button
            v-if="detail.status === 2"
            v-permission="'question:edit'"
            type="success"
            plain
            :icon="Top"
            @click="handleStatus(detail, 'on')"
          >
            上架
          </el-button>
          <el-button
            v-if="detail.status === 5"
            v-permission="'question:edit'"
            type="primary"
            plain
            :icon="Refresh"
            @click="handleResubmit(detail)"
          >
            重新提交
          </el-button>
          <el-button
            v-permission="'question:edit'"
            type="primary"
            plain
            :icon="Edit"
            @click="openEdit(detail)"
          >
            编辑
          </el-button>
          <el-button
            v-permission="'question:delete'"
            type="danger"
            plain
            :icon="Delete"
            @click="handleDelete(detail)"
          >
            删除
          </el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importVisible"
      title="批量导入题目"
      width="880px"
      align-center
      destroy-on-close
      @closed="resetImport"
    >
      <div class="import-format">
        <strong>Excel 字段要求</strong>
        <p>题干 / 分类 / 题型 / 难度 / 选项A~F / 正确答案 / 解析 / 标签</p>
        <p>分类会作为「目标学科」下的阶段；阶段不存在时自动创建。题型支持单选、多选、判断、简答，单次最多 500 道。</p>
      </div>

      <div class="import-subject">
        <span>目标学科</span>
        <el-select
          v-model="importSubjectId"
          placeholder="请选择目标学科"
          style="width: 260px"
          @change="handleImportSubjectChange"
        >
          <el-option
            v-for="subject in subjectOptions"
            :key="subject.id"
            :label="`${subject.icon} ${subject.name}`"
            :value="subject.id"
          />
        </el-select>
        <el-button link type="primary" :icon="Download" @click="handleTemplateDownload">
          下载导入模板
        </el-button>
      </div>

      <el-upload
        drag
        :show-file-list="false"
        accept=".xlsx,.xls"
        :disabled="importLoading || !importSubjectId"
        :http-request="handleImportRequest"
        class="import-upload"
      >
        <el-icon class="import-upload-icon"><Upload /></el-icon>
        <div class="el-upload__text">将 Excel 拖到此处，或 <em>点击选择文件</em></div>
      </el-upload>

      <div v-if="importFileName" class="import-file">已选择：{{ importFileName }}</div>

      <div v-loading="importLoading" class="import-preview">
        <template v-if="importPreview">
          <div class="import-summary">
            <span>
              共 {{ importPreview.total }} 道，有效 <strong>{{ importPreview.validCount }}</strong> 道
            </span>
            <span v-if="importPreview.invalidCount" class="import-invalid">
              无效 {{ importPreview.invalidCount }} 道
            </span>
            <span v-if="importPreview.rows.length < importPreview.validCount" class="import-more">
              仅预览前 {{ importPreview.rows.length }} 条有效数据
            </span>
          </div>
          <div v-if="importPreview.stagesToCreate.length" class="stages-create">
            将自动创建阶段：
            <el-tag
              v-for="stage in importPreview.stagesToCreate"
              :key="stage"
              size="small"
              type="warning"
              effect="plain"
            >
              {{ stage }}
            </el-tag>
          </div>

          <el-collapse v-if="importPreview.errors.length" class="import-errors">
            <el-collapse-item :title="`错误明细（${importPreview.errors.length} 条）`">
              <el-table :data="importPreview.errors" size="small" max-height="220">
                <el-table-column prop="rowNumber" label="行号" width="70" />
                <el-table-column prop="content" label="题干" min-width="220" show-overflow-tooltip />
                <el-table-column label="原因" min-width="260">
                  <template #default="{ row }">{{ row.messages.join('；') }}</template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>

          <el-table :data="importPreview.rows" size="small" max-height="300">
            <el-table-column prop="rowNumber" label="行号" width="70" />
            <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip />
            <el-table-column prop="stageName" label="阶段" width="150" show-overflow-tooltip />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ typeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column label="难度" width="90">
              <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button
          v-permission="'question:import'"
          type="primary"
          :icon="Check"
          :disabled="!importPreview?.token || importLoading"
          :loading="importConfirming"
          @click="handleConfirmImport"
        >
          确认导入{{ importPreview?.validCount ? ` ${importPreview.validCount} 道` : '' }}
        </el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.question-page {
  min-width: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 14px 16px;
  border-radius: var(--ys-radius-lg);
  background: #fff;
  box-shadow: var(--ys-shadow-sm);
  border-top: 3px solid var(--ys-primary);
}

.stat-card.tone-blue {
  border-top-color: #409eff;
}

.stat-card.tone-green {
  border-top-color: #22b07d;
}

.stat-card.tone-orange {
  border-top-color: #f5a623;
}

.stat-card.tone-primary {
  border-top-color: #667eea;
}

.stat-card.tone-violet {
  border-top-color: #a18cd1;
}

.stat-card.tone-cyan {
  border-top-color: #30cfd0;
}

.stat-card.tone-pink {
  border-top-color: #f093fb;
}

.stat-value {
  font-size: 23px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--ys-text-primary);
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.table-card {
  margin-top: 18px;
  padding: 18px 20px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 9px 12px;
  margin-bottom: 12px;
  border-radius: var(--ys-radius-md);
  background: #f0f9eb;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.batch-bar strong {
  color: var(--ys-text-primary);
}

.question-content {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ys-text-primary);
  cursor: pointer;
}

.question-content:hover {
  color: var(--ys-primary);
}

.question-tags {
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.row-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px 6px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

:deep(.question-actions-column) {
  border-left: 1px solid var(--ys-border-light) !important;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.option-editor {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--ys-bg);
  color: var(--ys-text-secondary);
  font-size: 12px;
}

.correct-control {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: none;
  min-width: 52px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.correct-control input[type='radio'] {
  accent-color: var(--ys-primary);
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-tags {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.detail-content {
  padding: 14px;
  border-radius: var(--ys-radius-md);
  background: #f8f9fc;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  color: var(--ys-text-primary);
}

.detail-options {
  margin-top: 12px;
}

.detail-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid var(--ys-border-light);
  border-radius: var(--ys-radius-sm);
  font-size: 13px;
}

.detail-option.is-correct {
  border-color: rgba(34, 176, 125, 0.45);
  background: rgba(34, 176, 125, 0.06);
}

.detail-block {
  margin-top: 12px;
}

.detail-block strong {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.detail-block p {
  margin: 0;
  padding: 10px 12px;
  border-radius: var(--ys-radius-sm);
  background: #f8f9fc;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  color: var(--ys-text-primary);
}

.reject-reason p {
  background: rgba(245, 108, 108, 0.07);
  color: var(--el-color-danger);
}

.detail-section-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.detail-section-bar {
  display: inline-block;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--ys-primary);
}

.detail-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 9px;
}

.detail-stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 74px;
  border-radius: var(--ys-radius-md);
  background: #f8f9fc;
}

.detail-stat-card strong {
  font-size: 25px;
  font-weight: 700;
  line-height: 1.2;
}

.detail-stat-card span {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.detail-stat-card--blue strong {
  color: #409eff;
}

.detail-stat-card--green strong {
  color: #22b07d;
}

.detail-stat-card--orange strong {
  color: #f5a623;
}

.detail-created {
  margin-top: 10px;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.import-format {
  padding: 12px 14px;
  margin-bottom: 12px;
  border-radius: var(--ys-radius-md);
  background: var(--ys-bg);
  font-size: 12px;
  line-height: 1.8;
  color: var(--ys-text-secondary);
}

.import-format strong {
  color: var(--ys-text-primary);
}

.import-format p {
  margin: 4px 0 0;
}

.import-subject {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.import-upload :deep(.el-upload-dragger) {
  padding: 18px;
}

.import-upload-icon {
  font-size: 28px;
  color: var(--ys-primary);
}

.import-file {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.import-preview {
  margin-top: 14px;
  min-height: 40px;
}

.import-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: var(--ys-radius-md);
  background: #f0f9eb;
  font-size: 13px;
  color: var(--ys-text-primary);
}

.import-invalid {
  color: var(--el-color-danger);
}

.import-more {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.stages-create {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.import-errors {
  margin-bottom: 10px;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-bar .el-input,
  .filter-bar .el-select {
    width: 100% !important;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>
