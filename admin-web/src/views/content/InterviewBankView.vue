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
  Delete,
  Download,
  Edit,
  Plus,
  RefreshLeft,
  Search,
  Top,
  Upload,
  View
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  changeInterviewQuestionStatus,
  confirmInterviewImport,
  createInterviewQuestion,
  createInterviewTopic,
  deleteInterviewQuestion,
  deleteInterviewTopic,
  downloadInterviewTemplate,
  getInterviewQuestion,
  listInterviewTopics,
  pageInterviewQuestions,
  previewInterviewImport,
  updateInterviewQuestion,
  updateInterviewTopic
} from '@/api/interview-bank'
import { listSubjectOptions } from '@/api/subject'
import type {
  InterviewImportPreview,
  InterviewQuestionDetail,
  InterviewQuestionItem,
  InterviewTopicItem
} from '@/types/interview-bank'
import type { SubjectOption } from '@/types/subject'

const subjects = ref<SubjectOption[]>([])
const questions = ref<InterviewQuestionItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const topics = ref<InterviewTopicItem[]>([])

const filters = reactive<{
  keyword: string
  subjectId?: number
  topic?: string
  difficulty?: number
}>({
  keyword: '',
  subjectId: undefined,
  topic: undefined,
  difficulty: undefined
})

const emptyText = computed(() =>
  filters.topic ? '该主题下还没有题目' : '没有符合条件的题目'
)

interface TopicFilterOption {
  name: string
  count: number
  inVocabulary: boolean
}

const filterTopicOptions = computed<TopicFilterOption[]>(() => {
  const map = new Map<string, TopicFilterOption>()
  topics.value.forEach((topic) => {
    const scopeMatched =
      topic.subjectId === null || !filters.subjectId || topic.subjectId === filters.subjectId
    if (!scopeMatched) {
      return
    }
    const existing = map.get(topic.name)
    if (existing) {
      existing.count += topic.questionCount
      existing.inVocabulary = existing.inVocabulary || topic.inVocabulary
    } else {
      map.set(topic.name, {
        name: topic.name,
        count: topic.questionCount,
        inVocabulary: topic.inVocabulary
      })
    }
  })
  return [...map.values()].sort((left, right) => right.count - left.count)
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<InterviewQuestionDetail | null>(null)

const editVisible = ref(false)
const editSubmitting = ref(false)
const editForm = reactive<{
  id: number
  subjectId: number | null
  content: string
  referenceAnswer: string | null
  topic: string
  difficulty: number
  sourceType: number
}>({
  id: 0,
  subjectId: null,
  content: '',
  referenceAnswer: null,
  topic: '',
  difficulty: 2,
  sourceType: 1
})

const createVisible = ref(false)
const createSubmitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<{
  subjectId?: number
  content: string
  referenceAnswer: string
  topic: string
  difficulty: number
  tags: string
}>({
  subjectId: undefined,
  content: '',
  referenceAnswer: '',
  topic: '',
  difficulty: 2,
  tags: ''
})

const createRules: FormRules = {
  subjectId: [
    {
      validator: (_rule, value: number | undefined, callback) => {
        if (!value) {
          callback(new Error('请选择所属学科'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  content: [
    { required: true, message: '请输入题干', trigger: 'blur' },
    { max: 10000, message: '题干不能超过 10000 个字符', trigger: 'blur' }
  ],
  tags: [{ max: 255, message: '标签不能超过 255 个字符', trigger: 'blur' }]
}

const importVisible = ref(false)
const importLoading = ref(false)
const importConfirming = ref(false)
const importFileName = ref('')
const importSubjectId = ref<number | undefined>()
const importPreview = ref<InterviewImportPreview | null>(null)

const topicVisible = ref(false)
const topicLoading = ref(false)
const topicSubmitting = ref(false)
const topicForm = reactive<{ id: number | null; name: string; subjectId?: number }>({
  id: null,
  name: '',
  subjectId: undefined
})

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
  await Promise.all([loadSubjects(), loadTopics(), loadQuestions()])
}

async function loadSubjects(): Promise<void> {
  subjects.value = await listSubjectOptions()
}

async function loadTopics(): Promise<void> {
  topics.value = await listInterviewTopics()
}

async function loadQuestions(): Promise<void> {
  loading.value = true
  try {
    const result = await pageInterviewQuestions({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      subjectId: filters.subjectId,
      topic: filters.topic,
      difficulty: filters.difficulty
    })
    questions.value = result.list
    total.value = result.total
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
  filters.topic = undefined
  filters.difficulty = undefined
  page.value = 1
  void loadQuestions().catch(() => undefined)
}

function handleSubjectFilterChange(): void {
  filters.topic = undefined
}

function handleSizeChange(): void {
  page.value = 1
  void loadQuestions().catch(() => undefined)
}

function subjectLabel(subject: SubjectOption): string {
  return subject.icon ? `${subject.icon} ${subject.name}` : subject.name
}

function topicOptionsFor(subjectId?: number | null): string[] {
  const names: string[] = []
  topics.value.forEach((topic) => {
    const scopeMatched =
      topic.subjectId === null || subjectId === undefined || subjectId === null || topic.subjectId === subjectId
    if (scopeMatched && !names.includes(topic.name)) {
      names.push(topic.name)
    }
  })
  return names
}

async function openDetail(row: InterviewQuestionItem): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getInterviewQuestion(row.id)
  } finally {
    detailLoading.value = false
  }
}

function openEdit(row: InterviewQuestionItem | InterviewQuestionDetail): void {
  editForm.id = row.id
  editForm.subjectId = row.subjectId
  editForm.content = row.content
  editForm.referenceAnswer = 'referenceAnswer' in row ? row.referenceAnswer : null
  editForm.topic = row.topic ?? ''
  editForm.difficulty = row.difficulty
  editForm.sourceType = row.sourceType
  editVisible.value = true
}

async function submitEdit(): Promise<void> {
  editSubmitting.value = true
  try {
    await updateInterviewQuestion(editForm.id, {
      topic: editForm.topic.trim() || null,
      difficulty: editForm.difficulty
    })
    ElMessage.success('已保存主题与难度')
    editVisible.value = false
    await refreshAfterChange()
  } finally {
    editSubmitting.value = false
  }
}

function openCreate(): void {
  Object.assign(createForm, {
    subjectId: filters.subjectId ?? subjects.value[0]?.id,
    content: '',
    referenceAnswer: '',
    topic: '',
    difficulty: 2,
    tags: ''
  })
  createVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

async function submitCreate(): Promise<void> {
  const form = createFormRef.value
  if (!form) {
    return
  }
  const valid = await form.validate().catch(() => false)
  if (!valid || !createForm.subjectId) {
    return
  }
  createSubmitting.value = true
  try {
    await createInterviewQuestion({
      subjectId: createForm.subjectId,
      content: createForm.content.trim(),
      referenceAnswer: createForm.referenceAnswer.trim() || undefined,
      topic: createForm.topic.trim() || undefined,
      difficulty: createForm.difficulty,
      tags: createForm.tags.trim() || undefined
    })
    ElMessage.success('面试题已录入')
    createVisible.value = false
    await refreshAfterChange()
  } finally {
    createSubmitting.value = false
  }
}

async function handleDelete(row: InterviewQuestionItem | InterviewQuestionDetail): Promise<void> {
  const tips: Record<number, string> = {
    1: '该题来自知识库文档解析，删除后不影响源文件。',
    2: '该题为手工录入，删除后不可恢复。',
    3: '该题由 Excel 导入，删除后源文件不受影响，但重新导入同一文件会再次入库。'
  }
  try {
    await ElMessageBox.confirm(
      `${tips[row.sourceType] ?? ''}删除后该题不再出现在面试题库与 AI 出题范围中，确定删除吗？`,
      '删除面试题',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteInterviewQuestion(row.id)
  ElMessage.success('已删除')
  detailVisible.value = false
  await refreshAfterChange()
}

async function handleStatus(row: InterviewQuestionItem | InterviewQuestionDetail, status: number): Promise<void> {
  if (status === 2) {
    try {
      await ElMessageBox.confirm(
        '下架后该题不再参与 AI 面试出题，可随时重新上架。确定下架吗？',
        '下架面试题',
        { type: 'warning', confirmButtonText: '下架', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  await changeInterviewQuestionStatus(row.id, status)
  ElMessage.success(status === 1 ? '已上架' : '已下架')
  detailVisible.value = false
  await refreshAfterChange()
}

async function refreshAfterChange(): Promise<void> {
  await Promise.all([loadQuestions(), loadTopics()])
  if (detailVisible.value && detail.value) {
    detail.value = await getInterviewQuestion(detail.value.id)
  }
}

function openImport(): void {
  resetImport()
  importSubjectId.value = undefined
  importVisible.value = true
}

function resetImport(): void {
  importLoading.value = false
  importConfirming.value = false
  importFileName.value = ''
  importPreview.value = null
}

async function handleTemplateDownload(): Promise<void> {
  const blob = await downloadInterviewTemplate()
  downloadBlob(blob, '面试题导入模板.xlsx')
}

async function handleImportRequest(options: UploadRequestOptions): Promise<void> {
  const file = options.file as File
  importFileName.value = file.name
  importLoading.value = true
  importPreview.value = null
  try {
    const preview = await previewInterviewImport(file, importSubjectId.value)
    importPreview.value = preview
    options.onSuccess?.(preview)
    if (preview.validCount > 0) {
      ElMessage.success(`解析完成，可导入 ${preview.validCount} 道`)
    } else {
      ElMessage.warning('没有可导入的有效面试题，请查看错误明细')
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
    const result = await confirmInterviewImport(token)
    ElMessage.success(`成功导入 ${result.imported} 道面试题`)
    importVisible.value = false
    await loadAll()
  } finally {
    importConfirming.value = false
  }
}

async function openTopics(): Promise<void> {
  topicVisible.value = true
  cancelTopicEdit()
  await loadTopics()
}

async function reloadTopicsInDialog(): Promise<void> {
  topicLoading.value = true
  try {
    await loadTopics()
  } finally {
    topicLoading.value = false
  }
}

function startTopicEdit(row: InterviewTopicItem): void {
  topicForm.id = row.id
  topicForm.name = row.name
  topicForm.subjectId = row.subjectId ?? undefined
}

function cancelTopicEdit(): void {
  topicForm.id = null
  topicForm.name = ''
  topicForm.subjectId = undefined
}

async function submitTopicForm(): Promise<void> {
  if (!topicForm.name.trim()) {
    ElMessage.warning('请输入主题名称')
    return
  }
  topicSubmitting.value = true
  try {
    const payload = {
      name: topicForm.name.trim(),
      subjectId: topicForm.subjectId ?? null
    }
    if (topicForm.id) {
      await updateInterviewTopic(topicForm.id, payload)
      ElMessage.success('主题已更新')
    } else {
      await createInterviewTopic(payload)
      ElMessage.success('主题已加入词表')
    }
    cancelTopicEdit()
    await reloadTopicsInDialog()
    await loadQuestions()
  } finally {
    topicSubmitting.value = false
  }
}

async function addTopicToVocabulary(row: InterviewTopicItem): Promise<void> {
  await createInterviewTopic({ name: row.name, subjectId: filters.subjectId ?? null })
  ElMessage.success(`已将「${row.name}」加入主题词表`)
  await reloadTopicsInDialog()
}

async function removeTopic(row: InterviewTopicItem): Promise<void> {
  if (!row.id) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除主题「${row.name}」吗？该主题下仍有题目时将无法删除。`,
      '删除主题',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteInterviewTopic(row.id)
  ElMessage.success('主题已删除')
  if (topicForm.id === row.id) {
    cancelTopicEdit()
  }
  await reloadTopicsInDialog()
}

function topicRowKey(row: InterviewTopicItem): string {
  return row.id ? `topic-${row.id}` : `out-${row.name}`
}

function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function sourceLabel(sourceType: number): string {
  return { 1: '文档解析', 2: '手工录入', 3: 'Excel 导入' }[sourceType] ?? '未知来源'
}

function sourceOriginLabel(sourceType: number): string {
  return sourceType === 2 ? '手工录入' : 'Excel 导入'
}

function sourceDetail(row: InterviewQuestionItem): string {
  if (row.sourceType === 2) {
    return '无来源文档'
  }
  const docName = row.sourceDocName || '未记录来源文件'
  return row.sourceSeq ? `${docName} · 第 ${row.sourceSeq} 题` : docName
}

function difficultyLabel(difficulty: number): string {
  return { 1: '简单', 2: '中等', 3: '困难' }[difficulty] ?? '-'
}

function difficultyTagType(difficulty: number): 'success' | 'warning' | 'danger' {
  return ({ 1: 'success', 2: 'warning', 3: 'danger' } as const)[difficulty] ?? 'warning'
}

function statusLabel(status: number): string {
  return { 1: '已上架', 2: '已下架' }[status] ?? '-'
}

function statusTagType(status: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}

function formatDateTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<template>
  <PageShell
    title="面试题库管理"
    description="跨学科的面试题总览：支持 Excel 批量导入、手工录入、主题与难度维护、上下架，题干与参考答案入库后只读。"
    :card="false"
  >
    <template #actions>
      <el-button v-permission="'interviewbank:upload'" :icon="Upload" @click="openImport">
        导入 Excel
      </el-button>
      <el-button
        v-permission="'interviewbank:upload'"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        录入题目
      </el-button>
    </template>

    <div class="interview-page">
      <div class="interview-tip">
        三种来源：知识库文档解析、手工录入、Excel 手动导入；题干与参考答案只读，仅可维护主题与难度。
        主题下拉包含 0 题主题，词表外主题会标注「词表外」。
      </div>

      <div class="table-card">
        <div class="filter-bar">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索题干 / 标签"
            clearable
            style="width: 230px"
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
              v-for="subject in subjects"
              :key="subject.id"
              :label="subjectLabel(subject)"
              :value="subject.id"
            />
          </el-select>
          <el-select
            v-model="filters.topic"
            placeholder="全部主题"
            clearable
            filterable
            style="width: 185px"
          >
            <el-option
              v-for="topic in filterTopicOptions"
              :key="topic.name"
              :label="`${topic.name}（${topic.count}）${topic.inVocabulary ? '' : ' · 词表外'}`"
              :value="topic.name"
            />
          </el-select>
          <el-select v-model="filters.difficulty" placeholder="全部难度" clearable style="width: 120px">
            <el-option label="简单" :value="1" />
            <el-option label="中等" :value="2" />
            <el-option label="困难" :value="3" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
          <el-button
            v-permission="'interviewbank:view'"
            link
            type="primary"
            class="topic-entry"
            @click="openTopics"
          >
            主题维护
          </el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="questions"
          row-key="id"
          :empty-text="emptyText"
        >
          <el-table-column label="题目" min-width="330">
            <template #default="{ row, $index }">
              <div class="question-head">
                <span class="question-seq">{{ (page - 1) * pageSize + $index + 1 }}</span>
                <span class="question-content" @click="openDetail(row)">{{ row.content }}</span>
              </div>
              <div class="question-meta">
                <el-tag
                  v-if="row.sourceType !== 1"
                  size="small"
                  effect="plain"
                  :type="row.sourceType === 2 ? 'success' : 'warning'"
                >
                  {{ sourceOriginLabel(row.sourceType) }}
                </el-tag>
                <span v-if="row.tags" class="question-tags">{{ row.tags }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="subjectName" label="所属学科" width="150" show-overflow-tooltip />
          <el-table-column label="来源" width="195">
            <template #default="{ row }">
              <div class="source-main">{{ sourceLabel(row.sourceType) }}</div>
              <div class="source-sub">{{ sourceDetail(row) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="difficultyTagType(row.difficulty)" size="small" effect="plain">
                {{ difficultyLabel(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="主题" width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.topic">{{ row.topic }}</span>
              <span v-else class="untagged">未分类</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="85" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="130">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button link type="primary" size="small" :icon="View" @click="openDetail(row)">
                  详情
                </el-button>
                <el-button
                  v-permission="'interviewbank:edit'"
                  link
                  type="primary"
                  size="small"
                  :icon="Edit"
                  @click="openEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 1"
                  v-permission="'interviewbank:edit'"
                  link
                  type="warning"
                  size="small"
                  :icon="Bottom"
                  @click="handleStatus(row, 2)"
                >
                  下架
                </el-button>
                <el-button
                  v-else
                  v-permission="'interviewbank:edit'"
                  link
                  type="success"
                  size="small"
                  :icon="Top"
                  @click="handleStatus(row, 1)"
                >
                  上架
                </el-button>
                <el-button
                  v-permission="'interviewbank:delete'"
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
      v-model="detailVisible"
      title="面试题详情"
      width="760px"
      align-center
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-meta">
            <el-tag effect="plain">{{ detail.subjectName }}</el-tag>
            <el-tag :type="difficultyTagType(detail.difficulty)" effect="plain">
              {{ difficultyLabel(detail.difficulty) }}
            </el-tag>
            <el-tag v-if="detail.topic" type="info" effect="plain">{{ detail.topic }}</el-tag>
            <el-tag v-else type="info" effect="plain">未分类</el-tag>
            <el-tag :type="statusTagType(detail.status)" effect="plain">
              {{ statusLabel(detail.status) }}
            </el-tag>
          </div>
          <div class="detail-content">{{ detail.content }}</div>
          <div class="detail-block">
            <strong>参考答案</strong>
            <p v-if="detail.referenceAnswer">{{ detail.referenceAnswer }}</p>
            <p v-else class="detail-empty">未填写参考答案</p>
          </div>
          <div v-if="detail.tags" class="detail-block">
            <strong>标签</strong>
            <p>{{ detail.tags }}</p>
          </div>
          <div class="detail-section-title">
            <span class="detail-section-bar" />
            来源信息
          </div>
          <div class="detail-source-grid">
            <div><span>来源</span><strong>{{ sourceLabel(detail.sourceType) }}</strong></div>
            <div><span>来源文件</span><strong>{{ detail.sourceDocName || '无' }}</strong></div>
            <div><span>文档内序号</span><strong>{{ detail.sourceSeq ? `第 ${detail.sourceSeq} 题` : '-' }}</strong></div>
            <div><span>创建时间</span><strong>{{ formatDateTime(detail.createdAt) }}</strong></div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="detail">
          <el-button
            v-if="detail.status === 1"
            v-permission="'interviewbank:edit'"
            type="warning"
            plain
            :icon="Bottom"
            @click="handleStatus(detail, 2)"
          >
            下架
          </el-button>
          <el-button
            v-else
            v-permission="'interviewbank:edit'"
            type="success"
            plain
            :icon="Top"
            @click="handleStatus(detail, 1)"
          >
            上架
          </el-button>
          <el-button
            v-permission="'interviewbank:edit'"
            type="primary"
            plain
            :icon="Edit"
            @click="openEdit(detail)"
          >
            编辑主题与难度
          </el-button>
          <el-button
            v-permission="'interviewbank:delete'"
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
      v-model="editVisible"
      title="编辑主题与难度"
      width="560px"
      align-center
      destroy-on-close
    >
      <div class="edit-lock-tip">
        题干与参考答案不随编辑修改（与来源保持一致）。
        {{ editForm.sourceType === 2 ? '如需改内容请删除后重新录入。' : '如需改内容请修正源文件后重新导入。' }}
      </div>
      <el-form label-position="top">
        <el-form-item label="题干">
          <div class="readonly-block">{{ editForm.content }}</div>
        </el-form-item>
        <el-form-item label="主题">
          <el-select
            v-model="editForm.topic"
            placeholder="未分类"
            clearable
            filterable
            allow-create
            default-first-option
            style="width: 100%"
          >
            <el-option
              v-for="name in topicOptionsFor(editForm.subjectId)"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
          <div class="hint">留空表示「未分类」；主题词表可在「主题维护」中管理。</div>
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="editForm.difficulty">
            <el-radio :value="1">简单</el-radio>
            <el-radio :value="2">中等</el-radio>
            <el-radio :value="3">困难</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="editSubmitting" @click="submitEdit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="createVisible"
      title="录入面试题"
      width="720px"
      align-center
      destroy-on-close
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="所属学科" prop="subjectId">
            <el-select v-model="createForm.subjectId" placeholder="请选择学科" style="width: 100%">
              <el-option
                v-for="subject in subjects"
                :key="subject.id"
                :label="subjectLabel(subject)"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-radio-group v-model="createForm.difficulty">
              <el-radio :value="1">简单</el-radio>
              <el-radio :value="2">中等</el-radio>
              <el-radio :value="3">困难</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="题干" prop="content" class="span-2">
            <el-input
              v-model="createForm.content"
              type="textarea"
              :rows="3"
              maxlength="10000"
              show-word-limit
              placeholder="题干会直接用于提问与语音播报，不要写「1.」这类题号前缀"
            />
          </el-form-item>
          <el-form-item label="参考答案" class="span-2">
            <el-input
              v-model="createForm.referenceAnswer"
              type="textarea"
              :rows="5"
              maxlength="10000"
              show-word-limit
              placeholder="参考答案是 AI 评判的唯一依据，可留空"
            />
          </el-form-item>
          <el-form-item label="主题">
            <el-select
              v-model="createForm.topic"
              placeholder="未分类（可留空）"
              clearable
              filterable
              allow-create
              default-first-option
              style="width: 100%"
            >
              <el-option
                v-for="name in topicOptionsFor(createForm.subjectId)"
                :key="name"
                :label="name"
                :value="name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签" prop="tags">
            <el-input v-model="createForm.tags" maxlength="255" placeholder="多个标签用逗号分隔，可留空" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="createSubmitting" @click="submitCreate">
          确定录入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importVisible"
      title="批量导入面试题"
      width="900px"
      align-center
      destroy-on-close
      @closed="resetImport"
    >
      <div class="import-format">
        <strong>Excel 字段要求</strong>
        <p>所属学科 / 题干 / 难度 / 主题 / 标签 / 参考答案</p>
        <p>
          「所属学科」需与学科管理中的名称一致，支持一份文件混填多个学科；难度填简单 / 中等 / 困难，留空按中等；
          主题可留空表示未分类；单次最多 500 道，单文件不超过 10MB。
        </p>
      </div>

      <div class="import-toolbar">
        <span>默认学科</span>
        <el-select
          v-model="importSubjectId"
          placeholder="Excel 未填学科时使用（可选）"
          clearable
          style="width: 280px"
        >
          <el-option
            v-for="subject in subjects"
            :key="subject.id"
            :label="subjectLabel(subject)"
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
        :disabled="importLoading"
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
            <span>共 {{ importPreview.total }} 行，可导入 <strong>{{ importPreview.validCount }}</strong> 道</span>
            <span v-if="importPreview.invalidCount" class="import-invalid">
              无效 {{ importPreview.invalidCount }} 行
            </span>
            <span v-if="importPreview.rows.length < importPreview.validCount" class="import-more">
              仅预览前 {{ importPreview.rows.length }} 条
            </span>
          </div>

          <div v-if="importPreview.subjects.length" class="import-subjects">
            涉及学科：
            <el-tag
              v-for="item in importPreview.subjects"
              :key="item.subjectId"
              size="small"
              effect="plain"
            >
              {{ item.subjectName }} {{ item.count }} 道
            </el-tag>
          </div>

          <div v-if="importPreview.topicsNotInVocabulary.length" class="import-topics">
            词表外主题（导入后可在主题维护中补充）：
            <el-tag
              v-for="topic in importPreview.topicsNotInVocabulary"
              :key="topic"
              size="small"
              type="warning"
              effect="plain"
            >
              {{ topic }}
            </el-tag>
          </div>

          <el-collapse v-if="importPreview.errors.length" class="import-errors">
            <el-collapse-item :title="`错误明细（${importPreview.errors.length} 行）`">
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
            <el-table-column prop="content" label="题干" min-width="280" show-overflow-tooltip />
            <el-table-column prop="subjectName" label="学科" width="150" show-overflow-tooltip />
            <el-table-column label="难度" width="80">
              <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
            </el-table-column>
            <el-table-column label="主题" width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.topic || '未分类' }}</template>
            </el-table-column>
            <el-table-column label="标签" width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.tags || '-' }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button
          v-permission="'interviewbank:upload'"
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

    <el-dialog
      v-model="topicVisible"
      title="面试题主题维护"
      width="820px"
      align-center
      destroy-on-close
      @closed="cancelTopicEdit"
    >
      <div class="topic-form">
        <el-input
          v-model="topicForm.name"
          maxlength="50"
          placeholder="主题名称，例如：大模型与本地部署"
          style="width: 300px"
          @keyup.enter="submitTopicForm"
        />
        <el-select
          v-model="topicForm.subjectId"
          placeholder="全局主题"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="subject in subjects"
            :key="subject.id"
            :label="subject.name"
            :value="subject.id"
          />
        </el-select>
        <el-button
          v-permission="'interviewbank:upload'"
          type="primary"
          :icon="topicForm.id ? Check : Plus"
          :loading="topicSubmitting"
          @click="submitTopicForm"
        >
          {{ topicForm.id ? '保存修改' : '加入词表' }}
        </el-button>
        <el-button v-if="topicForm.id" @click="cancelTopicEdit">取消编辑</el-button>
      </div>

      <el-table
        v-loading="topicLoading"
        :data="topics"
        :row-key="topicRowKey"
        size="small"
        max-height="380"
      >
        <el-table-column prop="name" label="主题" min-width="200" show-overflow-tooltip />
        <el-table-column label="所属学科" width="160">
          <template #default="{ row }">{{ row.subjectName || '全局主题' }}</template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题目数" width="90" align="center" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.inVocabulary ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.inVocabulary ? '词表内' : '词表外' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <template v-if="row.inVocabulary">
              <el-button
                v-permission="'interviewbank:edit'"
                link
                type="primary"
                size="small"
                :icon="Edit"
                @click="startTopicEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'interviewbank:delete'"
                link
                type="danger"
                size="small"
                :icon="Delete"
                @click="removeTopic(row)"
              >
                删除
              </el-button>
            </template>
            <el-button
              v-else
              v-permission="'interviewbank:upload'"
              link
              type="primary"
              size="small"
              :icon="Plus"
              @click="addTopicToVocabulary(row)"
            >
              加入词表
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="topicVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.interview-page {
  min-width: 0;
}

.interview-tip {
  padding: 10px 14px;
  margin-bottom: 14px;
  border-radius: var(--ys-radius-md);
  border-left: 3px solid var(--ys-primary);
  background: rgba(64, 158, 255, 0.06);
  font-size: 12px;
  line-height: 1.8;
  color: var(--ys-text-secondary);
}

.table-card {
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

.topic-entry {
  margin-left: auto;
}

.question-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.question-seq {
  flex: none;
  min-width: 22px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  line-height: 20px;
  color: var(--ys-text-secondary);
}

.question-content {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.55;
  color: var(--ys-text-primary);
  cursor: pointer;
}

.question-content:hover {
  color: var(--ys-primary);
}

.question-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
  padding-left: 30px;
}

.question-tags {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.source-main {
  font-size: 13px;
  color: var(--ys-text-primary);
}

.source-sub {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.untagged {
  font-size: 12px;
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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
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
  line-height: 1.75;
  white-space: pre-wrap;
  color: var(--ys-text-primary);
}

.detail-block p.detail-empty {
  color: var(--ys-text-secondary);
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

.detail-source-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 9px;
  padding: 12px 14px;
  border-radius: var(--ys-radius-md);
  background: #f8f9fc;
}

.detail-source-grid span {
  display: block;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.detail-source-grid strong {
  display: block;
  margin-top: 2px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ys-text-primary);
  overflow-wrap: anywhere;
}

.edit-lock-tip {
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: var(--ys-radius-md);
  background: var(--ys-bg);
  font-size: 12px;
  line-height: 1.7;
  color: var(--ys-text-secondary);
}

.readonly-block {
  width: 100%;
  max-height: 120px;
  overflow-y: auto;
  padding: 10px 12px;
  border-radius: var(--ys-radius-sm);
  background: #f8f9fc;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  color: var(--ys-text-secondary);
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

.import-toolbar {
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

.import-subjects,
.import-topics {
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

.topic-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-bottom: 14px;
  margin-bottom: 12px;
  border-bottom: 1px dashed var(--ys-border-light);
}

@media (max-width: 1280px) {
  .filter-bar :deep(.el-input),
  .filter-bar :deep(.el-select) {
    width: 160px !important;
  }

  .topic-entry {
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .filter-bar :deep(.el-input),
  .filter-bar :deep(.el-select) {
    width: 100% !important;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }

  .detail-source-grid {
    grid-template-columns: 1fr;
  }

  .topic-form :deep(.el-input),
  .topic-form :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
