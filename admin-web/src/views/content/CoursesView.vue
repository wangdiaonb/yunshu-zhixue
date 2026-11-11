<script setup lang="ts">
import { computed, markRaw, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions
} from 'element-plus'
import {
  Check,
  Clock,
  Collection,
  Delete,
  Download,
  Edit,
  Plus,
  RefreshLeft,
  Search,
  Upload,
  VideoCamera,
  ZoomIn
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  confirmCourseImport,
  createCourse,
  deleteCourse,
  downloadCourseImportTemplate,
  getCourseStats,
  pageCourses,
  previewCourseImport,
  updateCourse
} from '@/api/course'
import { listSubjectOptions } from '@/api/subject'
import type {
  CourseImportPreview,
  CourseItem,
  CourseSavePayload,
  CourseStats
} from '@/types/course'
import type { SubjectOption } from '@/types/subject'

const stats = ref<CourseStats>({
  totalCourses: 0,
  totalHours: 0,
  relatedSubjectCount: 0
})
const courses = ref<CourseItem[]>([])
const coverErrors = ref<Record<number, boolean>>({})
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const subjectOptions = ref<SubjectOption[]>([])

const filters = reactive<{
  keyword: string
  subjectId?: number
  source?: number
}>({
  keyword: '',
  subjectId: undefined,
  source: undefined
})

const statCards = computed(() => [
  {
    label: '课程总数',
    value: stats.value.totalCourses.toLocaleString(),
    icon: markRaw(VideoCamera),
    tone: 'blue'
  },
  {
    label: '总时长（小时）',
    value: Number(stats.value.totalHours).toFixed(1),
    icon: markRaw(Clock),
    tone: 'green'
  },
  {
    label: '关联学科数',
    value: stats.value.relatedSubjectCount.toLocaleString(),
    icon: markRaw(Collection),
    tone: 'violet'
  }
])

const courseDialogVisible = ref(false)
const courseDialogMode = ref<'create' | 'edit'>('create')
const editingCourseId = ref<number | null>(null)
const courseFormRef = ref<FormInstance>()
const courseSubmitting = ref(false)
const coverPreviewError = ref(false)
const courseForm = reactive({
  name: '',
  coverUrl: '',
  durationText: '',
  subjectId: 0,
  source: 1,
  link: '',
  extraSubjectIds: [] as number[]
})

const extraSubjectOptions = computed(() =>
  subjectOptions.value.filter((option) => option.id !== courseForm.subjectId)
)

const courseRules: FormRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { max: 100, message: '课程名称不能超过 100 个字符', trigger: 'blur' }
  ],
  durationText: [
    {
      validator: (_rule, value: string, callback) => {
        const minutes = parseDuration(value)
        if (minutes === null) {
          callback(new Error('请输入正确时长，格式为 小时:分钟，如 75:30'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  subjectId: [
    {
      validator: (_rule, value: number | null | undefined, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('请选择所属学科'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  coverUrl: [{ max: 500, message: '封面地址不能超过 500 个字符', trigger: 'blur' }],
  link: [{ max: 500, message: '课程链接不能超过 500 个字符', trigger: 'blur' }]
}

watch(
  () => courseForm.subjectId,
  (subjectId) => {
    courseForm.extraSubjectIds = courseForm.extraSubjectIds.filter((id) => id !== subjectId)
  }
)

watch(
  () => courseForm.coverUrl,
  () => {
    coverPreviewError.value = false
  }
)

const coverPreviewVisible = ref(false)
const coverPreviewUrl = ref('')
const coverPreviewTitle = ref('')

const importVisible = ref(false)
const importLoading = ref(false)
const importConfirming = ref(false)
const importFileName = ref('')
const importPreview = ref<CourseImportPreview | null>(null)

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
  await Promise.all([loadSubjectOptions(), loadCourses(), loadStats()])
}

async function loadSubjectOptions(): Promise<void> {
  subjectOptions.value = await listSubjectOptions()
}

async function loadStats(): Promise<void> {
  stats.value = await getCourseStats()
}

async function loadCourses(): Promise<void> {
  loading.value = true
  try {
    const result = await pageCourses({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword.trim() || undefined,
      subjectId: filters.subjectId,
      source: filters.source
    })
    courses.value = result.list
    coverErrors.value = {}
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
    void loadCourses().catch(() => undefined)
  }, 300)
}

function handleSearch(): void {
  page.value = 1
  void loadCourses().catch(() => undefined)
}

function resetFilters(): void {
  filters.keyword = ''
  filters.subjectId = undefined
  filters.source = undefined
  page.value = 1
  void loadCourses().catch(() => undefined)
}

function handleSizeChange(): void {
  page.value = 1
  void loadCourses().catch(() => undefined)
}

function openCreateCourse(): void {
  courseDialogMode.value = 'create'
  editingCourseId.value = null
  Object.assign(courseForm, {
    name: '',
    coverUrl: '',
    durationText: '',
    subjectId: 0,
    source: 1,
    link: '',
    extraSubjectIds: []
  })
  courseDialogVisible.value = true
  nextTick(() => courseFormRef.value?.clearValidate())
}

function openEditCourse(course: CourseItem): void {
  courseDialogMode.value = 'edit'
  editingCourseId.value = course.id
  Object.assign(courseForm, {
    name: course.name,
    coverUrl: course.coverUrl ?? '',
    durationText: formatDurationInput(course.durationMinutes),
    subjectId: course.subjectId ?? 0,
    source: course.source,
    link: course.link ?? '',
    extraSubjectIds: course.subjectIds.filter((id) => id !== course.subjectId)
  })
  courseDialogVisible.value = true
  nextTick(() => courseFormRef.value?.clearValidate())
}

async function submitCourse(): Promise<void> {
  const formInstance = courseFormRef.value
  if (!formInstance) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid) {
    return
  }
  const durationMinutes = parseDuration(courseForm.durationText)
  if (durationMinutes === null) {
    return
  }
  courseSubmitting.value = true
  try {
    const payload: CourseSavePayload = {
      name: courseForm.name.trim(),
      coverUrl: courseForm.coverUrl.trim() || undefined,
      durationMinutes,
      source: courseForm.source,
      link: courseForm.link.trim() || undefined,
      subjectId: courseForm.subjectId,
      subjectIds: courseForm.extraSubjectIds
    }
    if (courseDialogMode.value === 'create') {
      await createCourse(payload)
      ElMessage.success('课程创建成功')
    } else if (editingCourseId.value) {
      await updateCourse(editingCourseId.value, payload)
      ElMessage.success('课程信息已更新')
    }
    courseDialogVisible.value = false
    await loadAll()
  } finally {
    courseSubmitting.value = false
  }
}

async function handleDelete(course: CourseItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除课程「${course.name}」吗？被学习路线引用的课程无法删除。`,
      '删除课程',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteCourse(course.id)
  ElMessage.success('课程已删除')
  await loadAll()
}

function openImport(): void {
  resetImport()
  importVisible.value = true
}

function resetImport(): void {
  importLoading.value = false
  importConfirming.value = false
  importFileName.value = ''
  importPreview.value = null
}

async function handleDownloadTemplate(): Promise<void> {
  const blob = await downloadCourseImportTemplate()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '课程导入模板.xlsx'
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function handleImportRequest(options: UploadRequestOptions): Promise<void> {
  const file = options.file as File
  importFileName.value = file.name
  importLoading.value = true
  importPreview.value = null
  try {
    const preview = await previewCourseImport(file)
    importPreview.value = preview
    options.onSuccess?.(preview)
    if (preview.validCount > 0) {
      ElMessage.success(`解析完成，有效 ${preview.validCount} 条`)
    } else {
      ElMessage.warning('没有可导入的有效数据')
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
    const result = await confirmCourseImport(token)
    ElMessage.success(`成功导入 ${result.imported} 门课程`)
    importVisible.value = false
    await loadAll()
  } finally {
    importConfirming.value = false
  }
}

function coverSrc(url: string | null): string {
  if (!url) {
    return ''
  }
  // B 站图片 CDN 支持 HTTPS；同时避免 HTTPS 页面加载 HTTP 资源被拦截
  return /^http:\/\/.*hdslb\.com/i.test(url) ? url.replace(/^http:/i, 'https:') : url
}

function handleCoverError(courseId: number): void {
  coverErrors.value[courseId] = true
}

function openCoverPreview(course: CourseItem): void {
  if (!course.coverUrl || coverErrors.value[course.id]) {
    return
  }
  coverPreviewUrl.value = course.coverUrl
  coverPreviewTitle.value = course.name
  coverPreviewVisible.value = true
}

function parseDuration(value: string): number | null {
  if (!value) {
    return null
  }
  const match = /^(\d{1,4}):([0-5]?\d)$/.exec(value.trim())
  if (!match) {
    return null
  }
  const minutes = Number(match[1]) * 60 + Number(match[2])
  return minutes > 0 && minutes <= 100000 ? minutes : null
}

function formatDurationInput(minutes: number): string {
  return `${Math.floor(minutes / 60)}:${String(minutes % 60).padStart(2, '0')}`
}

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const remain = minutes % 60
  return hours > 0 ? `${hours} 小时 ${remain} 分` : `${remain} 分钟`
}
</script>

<template>
  <PageShell
    title="B站课程库"
    description="维护课程名称、封面、时长、来源与学科关联，支持 Excel 批量导入。"
    :card="false"
  >
    <template #actions>
      <el-button v-permission="'course:add'" :icon="Upload" @click="openImport">批量导入</el-button>
      <el-button v-permission="'course:add'" type="primary" :icon="Plus" @click="openCreateCourse">
        新增课程
      </el-button>
    </template>

    <div v-loading="loading" class="course-page">
      <div class="stats-grid">
        <div v-for="stat in statCards" :key="stat.label" class="stat-card">
          <div class="stat-icon" :class="`tone-${stat.tone}`">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <div class="table-card">
        <div class="filter-bar">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索课程名称 / BV号"
            clearable
            style="width: 240px"
            :prefix-icon="Search"
            @input="handleKeywordInput"
            @keyup.enter="handleSearch"
          />
          <el-select v-model="filters.subjectId" placeholder="全部学科" clearable style="width: 190px">
            <el-option
              v-for="subject in subjectOptions"
              :key="subject.id"
              :label="`${subject.icon} ${subject.name}`"
              :value="subject.id"
            />
            <el-option label="其他学科" :value="0" />
          </el-select>
          <el-select v-model="filters.source" placeholder="全部来源" clearable style="width: 130px">
            <el-option label="B站" :value="1" />
            <el-option label="其他" :value="2" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="courses" row-key="id" empty-text="暂无匹配的课程，试试调整筛选条件">
          <el-table-column label="序号" width="70" align="center">
            <template #default="{ $index }">
              {{ (page - 1) * pageSize + $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="封面" width="160">
            <template #default="{ row }">
              <div
                v-if="row.coverUrl && !coverErrors[row.id]"
                class="cover-cell"
                title="点击预览封面"
                @click="openCoverPreview(row)"
              >
                <img
                  :src="coverSrc(row.coverUrl)"
                  class="course-cover"
                  referrerpolicy="no-referrer"
                  loading="lazy"
                  alt=""
                  @error="handleCoverError(row.id)"
                />
                <span class="cover-zoom"><el-icon><ZoomIn /></el-icon></span>
              </div>
              <div v-else class="cover-empty">无封面</div>
            </template>
          </el-table-column>
          <el-table-column label="课程名称" min-width="260">
            <template #default="{ row }">
              <div class="course-name">{{ row.name }}</div>
              <div v-if="row.bvid" class="course-bvid">{{ row.bvid }}</div>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="130">
            <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
          </el-table-column>
          <el-table-column label="学科" width="200">
            <template #default="{ row }">
              <div class="subject-cell">
                <el-tag size="small" effect="plain">{{ row.subjectName }}</el-tag>
                <el-tooltip
                  v-if="row.subjectNames.length > 1"
                  :content="row.subjectNames.join('、')"
                  placement="top"
                >
                  <span class="extra-count">+{{ row.subjectNames.length - 1 }}</span>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.source === 1 ? 'primary' : 'info'" effect="plain" size="small">
                {{ row.source === 1 ? 'B站' : '其他' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                v-permission="'course:edit'"
                link
                type="primary"
                size="small"
                :icon="Edit"
                @click="openEditCourse(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'course:delete'"
                link
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
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
            @current-change="loadCourses"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="courseDialogVisible"
      :title="courseDialogMode === 'create' ? '新增课程' : '编辑课程'"
      width="680px"
      align-center
      destroy-on-close
    >
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="课程名称" prop="name" class="span-2">
            <el-input v-model="courseForm.name" maxlength="100" placeholder="请输入课程名称" />
          </el-form-item>
          <el-form-item label="封面图片 URL" prop="coverUrl" class="span-2">
            <el-input v-model="courseForm.coverUrl" maxlength="500" placeholder="https://..." />
            <div v-if="courseForm.coverUrl" class="cover-preview">
              <img
                v-if="!coverPreviewError"
                :src="coverSrc(courseForm.coverUrl)"
                class="form-cover"
                referrerpolicy="no-referrer"
                alt=""
                @error="coverPreviewError = true"
              />
              <span v-else class="cover-error">封面地址无效</span>
            </div>
          </el-form-item>
          <el-form-item label="时长" prop="durationText">
            <el-input v-model="courseForm.durationText" placeholder="例如 75:30" />
            <div class="hint">格式：小时:分钟，例如 75:30</div>
          </el-form-item>
          <el-form-item label="来源" prop="source">
            <el-radio-group v-model="courseForm.source">
              <el-radio :value="1">B站</el-radio>
              <el-radio :value="2">其他</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="所属学科" prop="subjectId">
            <el-select v-model="courseForm.subjectId" style="width: 100%">
              <el-option
                v-for="subject in subjectOptions"
                :key="subject.id"
                :label="`${subject.icon} ${subject.name}`"
                :value="subject.id"
              />
              <el-option label="📚 其他学科" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联学科（可选）">
            <el-select
              v-model="courseForm.extraSubjectIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="可关联多个额外学科"
              style="width: 100%"
            >
              <el-option
                v-for="subject in extraSubjectOptions"
                :key="subject.id"
                :label="`${subject.icon} ${subject.name}`"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="课程链接" prop="link" class="span-2">
            <el-input v-model="courseForm.link" maxlength="500" placeholder="https://www.bilibili.com/video/BV..." />
            <div class="hint">B站链接会自动解析 BV 号</div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="courseSubmitting" @click="submitCourse">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="coverPreviewVisible"
      :title="coverPreviewTitle"
      width="760px"
      align-center
      class="cover-preview-dialog"
    >
      <div class="cover-large-wrap">
        <img
          :src="coverSrc(coverPreviewUrl)"
          class="cover-large"
          referrerpolicy="no-referrer"
          alt="课程封面"
        />
      </div>
    </el-dialog>

    <el-dialog
      v-model="importVisible"
      title="批量导入课程"
      width="860px"
      align-center
      destroy-on-close
      @closed="resetImport"
    >
      <div class="import-format">
        <strong>Excel 字段要求</strong>
        <p>课程名称 / 所属学科 / 来源 / 时长（分钟） / BV号 / 封面 / 播放链接</p>
        <p>所属学科可用英文逗号填写多个；「其他学科」自动归入兜底学科；单次最多 1000 条。</p>
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

      <div class="import-template">
        <span>没有现成的表格？下载模板后按列填写即可：</span>
        <el-button link type="primary" :icon="Download" @click="handleDownloadTemplate">
          下载导入模板
        </el-button>
      </div>

      <div v-if="importFileName" class="import-file">已选择：{{ importFileName }}</div>

      <div v-loading="importLoading" class="import-preview">
        <template v-if="importPreview">
          <div class="import-summary">
            <span>共 {{ importPreview.total }} 条，有效 <strong>{{ importPreview.validCount }}</strong> 条</span>
            <span v-if="importPreview.invalidCount" class="import-invalid">
              无效 {{ importPreview.invalidCount }} 条
            </span>
            <span v-if="importPreview.rows.length < importPreview.validCount" class="import-more">
              仅预览前 {{ importPreview.rows.length }} 条有效数据
            </span>
          </div>

          <el-collapse v-if="importPreview.errors.length" class="import-errors">
            <el-collapse-item :title="`错误明细（${importPreview.errors.length} 条）`">
              <el-table :data="importPreview.errors" size="small" max-height="220">
                <el-table-column prop="rowNumber" label="行号" width="70" />
                <el-table-column prop="name" label="课程名称" min-width="200" />
                <el-table-column label="原因" min-width="260">
                  <template #default="{ row }">{{ row.messages.join('；') }}</template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>

          <el-table :data="importPreview.rows" size="small" max-height="300">
            <el-table-column prop="rowNumber" label="行号" width="70" />
            <el-table-column prop="name" label="课程名称" min-width="220" />
            <el-table-column label="学科" width="180">
              <template #default="{ row }">{{ row.subjectNames.join('、') }}</template>
            </el-table-column>
            <el-table-column label="时长" width="110">
              <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.warnings.length" size="small" type="warning" effect="plain">警告</el-tag>
                <el-tag v-else size="small" type="success" effect="plain">有效</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button
          v-permission="'course:add'"
          type="primary"
          :icon="Check"
          :disabled="!importPreview?.token || importLoading"
          :loading="importConfirming"
          @click="handleConfirmImport"
        >
          确认导入{{ importPreview?.validCount ? ` ${importPreview.validCount} 条` : '' }}
        </el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.course-page {
  min-width: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.tone-green {
  color: #22b07d;
  background: rgba(34, 176, 125, 0.12);
}

.tone-violet {
  color: #7c6cf0;
  background: rgba(124, 108, 240, 0.12);
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

.table-card {
  margin-top: 20px;
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
  margin-bottom: 16px;
}

.cover-cell {
  position: relative;
  width: 128px;
  height: 72px;
  overflow: hidden;
  border-radius: 6px;
  cursor: zoom-in;
  background: #f5f7fa;
}

.course-cover {
  display: block;
  width: 128px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f7fa;
}

.cover-zoom {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.38);
  color: #fff;
  font-size: 20px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.cover-cell:hover .cover-zoom {
  opacity: 1;
}

.cover-large-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  max-height: 70vh;
  overflow: hidden;
  border-radius: var(--ys-radius-md);
  background: #f5f7fa;
}

.cover-large {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 128px;
  height: 72px;
  border-radius: 6px;
  background: #f5f7fa;
  color: var(--ys-text-secondary);
  font-size: 12px;
}

.course-name {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ys-text-primary);
}

.course-bvid {
  margin-top: 3px;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.subject-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.extra-count {
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--ys-bg);
  color: var(--ys-text-secondary);
  font-size: 11px;
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

.cover-preview {
  margin-top: 8px;
}

.form-cover {
  display: block;
  width: 160px;
  height: 90px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f7fa;
}

.cover-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160px;
  height: 90px;
  color: var(--ys-text-secondary);
  font-size: 12px;
}

.import-format {
  padding: 12px 14px;
  margin-bottom: 14px;
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

.import-upload :deep(.el-upload-dragger) {
  padding: 18px;
}

.import-upload-icon {
  font-size: 28px;
  color: var(--ys-primary);
}

.import-template {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--ys-text-secondary);
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
  gap: 14px;
  flex-wrap: wrap;
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
  color: var(--ys-text-secondary);
  font-size: 12px;
}

.import-errors {
  margin-bottom: 10px;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .filter-bar {
    align-items: stretch;
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
