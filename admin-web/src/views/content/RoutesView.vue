<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Bottom,
  Check,
  Delete,
  Edit,
  Plus,
  RefreshLeft,
  Search,
  Top,
  View
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  addRouteCourses,
  createRoute,
  deleteRoute,
  getRouteDetail,
  listRouteCourseOptions,
  listRoutes,
  removeRouteCourse,
  sortRouteCourses,
  updateRoute,
  updateRouteStatus
} from '@/api/route'
import { listSubjectOptions } from '@/api/subject'
import type {
  RouteCard,
  RouteCourseOption,
  RouteDetail,
  RouteSavePayload
} from '@/types/route'
import type { SubjectOption } from '@/types/subject'

const loading = ref(false)
const routes = ref<RouteCard[]>([])
const subjectOptions = ref<SubjectOption[]>([])
const filters = reactive<{
  keyword: string
  subjectId?: number
  status?: number
}>({
  keyword: '',
  subjectId: undefined,
  status: undefined
})

const routeDialogVisible = ref(false)
const routeDialogMode = ref<'create' | 'edit'>('create')
const editingRouteId = ref<number | null>(null)
const routeFormRef = ref<FormInstance>()
const routeSubmitting = ref(false)
const routeForm = reactive({
  name: '',
  subjectId: 0,
  status: 1,
  icon: '🗺️',
  targetEducation: '',
  description: ''
})

const routeRules: FormRules = {
  name: [
    { required: true, message: '请输入路线名称', trigger: 'blur' },
    { max: 100, message: '路线名称不能超过 100 个字符', trigger: 'blur' }
  ],
  subjectId: [
    {
      validator: (_rule, value: number, callback) => {
        if (!value) {
          callback(new Error('请选择关联学科'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  targetEducation: [{ max: 100, message: '适合学历不能超过 100 个字符', trigger: 'blur' }],
  icon: [{ max: 50, message: '路线图标不能超过 50 个字符', trigger: 'blur' }],
  description: [{ max: 2000, message: '路线描述不能超过 2000 个字符', trigger: 'blur' }]
}

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRouteId = ref<number | null>(null)
const routeDetail = ref<RouteDetail | null>(null)
const coverErrors = ref<Record<number, boolean>>({})

const pickerVisible = ref(false)
const pickerLoading = ref(false)
const pickerSubmitting = ref(false)
const courseOptions = ref<RouteCourseOption[]>([])
const courseKeyword = ref('')
const selectedCourseIds = ref<number[]>([])

const filteredLocalCourses = computed(() =>
  filterCourseOptions(courseOptions.value.filter((course) => course.localSubject))
)
const filteredOtherCourses = computed(() =>
  filterCourseOptions(courseOptions.value.filter((course) => !course.localSubject))
)

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
  subjectOptions.value = await listSubjectOptions()
  await loadRoutes()
}

async function loadRoutes(): Promise<void> {
  loading.value = true
  try {
    routes.value = await listRoutes({
      keyword: filters.keyword.trim() || undefined,
      subjectId: filters.subjectId,
      status: filters.status
    })
  } finally {
    loading.value = false
  }
}

function handleKeywordInput(): void {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
  keywordTimer = setTimeout(() => {
    void loadRoutes().catch(() => undefined)
  }, 300)
}

function handleSearch(): void {
  if (keywordTimer) {
    clearTimeout(keywordTimer)
  }
  void loadRoutes().catch(() => undefined)
}

function resetFilters(): void {
  filters.keyword = ''
  filters.subjectId = undefined
  filters.status = undefined
  void loadRoutes().catch(() => undefined)
}

function openCreateRoute(): void {
  routeDialogMode.value = 'create'
  editingRouteId.value = null
  Object.assign(routeForm, {
    name: '',
    subjectId: subjectOptions.value[0]?.id ?? 0,
    status: 1,
    icon: '🗺️',
    targetEducation: '',
    description: ''
  })
  routeDialogVisible.value = true
  nextTick(() => routeFormRef.value?.clearValidate())
}

function openEditRoute(route: RouteCard): void {
  routeDialogMode.value = 'edit'
  editingRouteId.value = route.id
  Object.assign(routeForm, {
    name: route.name,
    subjectId: route.subjectId,
    status: route.status,
    icon: route.icon ?? '🗺️',
    targetEducation: route.targetEducation ?? '',
    description: route.description ?? ''
  })
  routeDialogVisible.value = true
  nextTick(() => routeFormRef.value?.clearValidate())
}

async function submitRoute(): Promise<void> {
  const formInstance = routeFormRef.value
  if (!formInstance) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid) {
    return
  }
  routeSubmitting.value = true
  try {
    const payload: RouteSavePayload = {
      name: routeForm.name.trim(),
      subjectId: routeForm.subjectId,
      status: routeForm.status,
      icon: routeForm.icon.trim() || undefined,
      targetEducation: routeForm.targetEducation.trim() || undefined,
      description: routeForm.description.trim() || undefined
    }
    if (routeDialogMode.value === 'create') {
      await createRoute(payload)
      ElMessage.success('学习路线已创建')
    } else if (editingRouteId.value) {
      await updateRoute(editingRouteId.value, payload)
      ElMessage.success('学习路线已更新')
    }
    routeDialogVisible.value = false
    await loadRoutes()
    if (detailRouteId.value && editingRouteId.value === detailRouteId.value && detailVisible.value) {
      await loadDetail(detailRouteId.value)
    }
  } finally {
    routeSubmitting.value = false
  }
}

async function handleRouteStatus(route: RouteCard, value: string | number | boolean): Promise<void> {
  const status = value ? 1 : 0
  try {
    await updateRouteStatus(route.id, status)
    ElMessage.success(status === 1 ? '路线已启用' : '路线已停用')
  } finally {
    await loadRoutes()
  }
}

async function removeRoute(route: RouteCard): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除学习路线「${route.name}」吗？路线下的课程关联会一并删除。`,
      '删除学习路线',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteRoute(route.id)
  ElMessage.success('学习路线已删除')
  if (detailRouteId.value === route.id) {
    detailVisible.value = false
  }
  await loadRoutes()
}

async function openDetail(route: RouteCard): Promise<void> {
  detailRouteId.value = route.id
  detailVisible.value = true
  routeDetail.value = null
  await loadDetail(route.id)
}

async function loadDetail(routeId: number): Promise<void> {
  detailLoading.value = true
  try {
    const data = await getRouteDetail(routeId)
    routeDetail.value = data
    coverErrors.value = {}
  } finally {
    detailLoading.value = false
  }
}

async function openCoursePicker(): Promise<void> {
  const route = routeDetail.value
  if (!route) {
    return
  }
  selectedCourseIds.value = []
  courseKeyword.value = ''
  courseOptions.value = []
  pickerVisible.value = true
  pickerLoading.value = true
  try {
    courseOptions.value = await listRouteCourseOptions(route.id)
  } finally {
    pickerLoading.value = false
  }
}

async function confirmAddCourses(): Promise<void> {
  const route = routeDetail.value
  if (!route || selectedCourseIds.value.length === 0) {
    return
  }
  pickerSubmitting.value = true
  try {
    await addRouteCourses(route.id, selectedCourseIds.value)
    ElMessage.success(`已添加 ${selectedCourseIds.value.length} 门课程`)
    pickerVisible.value = false
    await loadDetail(route.id)
    await loadRoutes()
  } finally {
    pickerSubmitting.value = false
  }
}

async function removeDetailCourse(courseId: number, courseName: string): Promise<void> {
  const route = routeDetail.value
  if (!route) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定要从路线中移除课程「${courseName}」吗？`, '移除课程', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await removeRouteCourse(route.id, courseId)
  ElMessage.success('课程已从路线中移除')
  await loadDetail(route.id)
  await loadRoutes()
}

async function moveDetailCourse(index: number, offset: number): Promise<void> {
  const route = routeDetail.value
  if (!route) {
    return
  }
  const target = index + offset
  if (target < 0 || target >= route.courses.length) {
    return
  }
  const ids = route.courses.map((course) => course.routeCourseId)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await sortRouteCourses(route.id, ids)
  await loadDetail(route.id)
  await loadRoutes()
}

function filterCourseOptions(options: RouteCourseOption[]): RouteCourseOption[] {
  const keyword = courseKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return options
  }
  return options.filter(
    (course) =>
      course.name.toLowerCase().includes(keyword) ||
      (course.bvid ?? '').toLowerCase().includes(keyword) ||
      course.subjectName.toLowerCase().includes(keyword)
  )
}

function formatDuration(minutes: number | null | undefined): string {
  const value = minutes ?? 0
  const hours = Math.floor(value / 60)
  const remain = value % 60
  return hours > 0 ? `${hours} 小时 ${remain} 分` : `${remain} 分钟`
}

function courseCover(url: string | null): string {
  if (!url) {
    return ''
  }
  return /^http:\/\/.*hdslb\.com/i.test(url) ? url.replace(/^http:/i, 'https:') : url
}

function handleCoverError(courseId: number): void {
  coverErrors.value[courseId] = true
}
</script>

<template>
  <PageShell
    title="学习路线"
    description="维护路线基本信息与课程学习顺序，支持跨学科补充课程、启停与学员视角预览。"
    :card="false"
  >
    <template #actions>
      <el-button v-permission="'route:add'" type="primary" :icon="Plus" @click="openCreateRoute">
        新增路线
      </el-button>
    </template>

    <div class="route-page">
      <div class="filter-card">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索路线名称 / 描述"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @input="handleKeywordInput"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.subjectId" placeholder="全部学科" clearable style="width: 180px">
          <el-option
            v-for="subject in subjectOptions"
            :key="subject.id"
            :label="`${subject.icon} ${subject.name}`"
            :value="subject.id"
          />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="启用" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
        <span class="filter-count">共 {{ routes.length }} 条路线</span>
      </div>

      <div v-loading="loading" class="route-grid">
        <article v-for="route in routes" :key="route.id" class="route-card">
          <div class="route-card__top">
            <span class="route-icon">{{ route.icon || route.subjectIcon || '🗺️' }}</span>
            <div class="route-title-wrap">
              <div class="route-title">
                <h3>{{ route.name }}</h3>
                <el-tag
                  :type="route.status === 1 ? 'success' : 'info'"
                  size="small"
                  effect="plain"
                >
                  {{ route.status === 1 ? '启用中' : '已停用' }}
                </el-tag>
              </div>
              <div class="route-subtitle">
                <span>{{ route.subjectIcon }} {{ route.subjectName }}</span>
                <span v-if="route.targetEducation">适合 {{ route.targetEducation }}</span>
              </div>
            </div>
            <div class="route-card__actions">
              <el-button
                v-permission="'route:edit'"
                text
                circle
                size="small"
                :icon="Edit"
                title="编辑路线"
                @click="openEditRoute(route)"
              />
              <el-button
                v-permission="'route:delete'"
                text
                circle
                size="small"
                type="danger"
                :icon="Delete"
                title="删除路线"
                @click="removeRoute(route)"
              />
            </div>
          </div>

          <p class="route-description">{{ route.description || '暂无路线描述' }}</p>

          <div class="route-meta">
            <span>共 <strong>{{ route.courseCount }}</strong> 门课程</span>
            <span>总计时长 <strong>{{ formatDuration(route.totalDurationMinutes) }}</strong></span>
          </div>

          <div class="route-preview-list">
            <div v-if="!route.previewCourses.length" class="route-preview-empty">
              暂无课程，进入详情从课程库添加
            </div>
            <div v-for="(course, index) in route.previewCourses" :key="course.routeCourseId" class="preview-row">
              <span class="preview-index">{{ index + 1 }}</span>
              <span class="preview-name">{{ course.name }}</span>
              <span class="preview-duration">{{ formatDuration(course.durationMinutes) }}</span>
            </div>
            <div v-if="route.courseCount > route.previewCourses.length" class="preview-more">
              还有 {{ route.courseCount - route.previewCourses.length }} 门课程…
            </div>
          </div>

          <div class="route-card__footer">
            <div class="route-status-switch">
              <el-switch
                v-permission="'route:edit'"
                :model-value="route.status === 1"
                size="small"
                @change="handleRouteStatus(route, $event)"
              />
              <span>{{ route.status === 1 ? '已启用' : '已停用' }}</span>
            </div>
            <el-button link type="primary" :icon="View" @click="openDetail(route)">预览 / 课程管理</el-button>
          </div>
        </article>

        <el-empty
          v-if="!loading && routes.length === 0"
          class="route-empty"
          description="暂无学习路线，点击上方「新增路线」创建一条"
        />
      </div>
    </div>

    <el-dialog
      v-model="routeDialogVisible"
      :title="routeDialogMode === 'create' ? '新增学习路线' : '编辑学习路线'"
      width="640px"
      align-center
      destroy-on-close
    >
      <el-form ref="routeFormRef" :model="routeForm" :rules="routeRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="路线名称" prop="name">
            <el-input v-model="routeForm.name" maxlength="100" placeholder="请输入路线名称" />
          </el-form-item>
          <el-form-item label="关联学科" prop="subjectId">
            <el-select v-model="routeForm.subjectId" style="width: 100%" placeholder="请选择学科">
              <el-option
                v-for="subject in subjectOptions"
                :key="subject.id"
                :label="`${subject.icon} ${subject.name}`"
                :value="subject.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="routeForm.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="路线图标（Emoji）" prop="icon">
            <el-input v-model="routeForm.icon" maxlength="8" placeholder="例如：🚀">
              <template #prepend><span class="emoji-preview">{{ routeForm.icon || '🗺️' }}</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="适合学历" prop="targetEducation">
            <el-input v-model="routeForm.targetEducation" maxlength="100" placeholder="例如：本科 / 不限" />
          </el-form-item>
          <el-form-item label="路线描述" prop="description" class="span-2">
            <el-input
              v-model="routeForm.description"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="请输入路线描述"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="routeDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="routeSubmitting" @click="submitRoute">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailVisible"
      :title="routeDetail?.name ?? '学习路线详情'"
      width="920px"
      align-center
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="routeDetail">
          <div class="detail-hero">
            <span class="detail-icon">{{ routeDetail.icon || routeDetail.subjectIcon || '🗺️' }}</span>
            <div class="detail-main">
              <h3>
                {{ routeDetail.name }}
                <el-tag
                  :type="routeDetail.status === 1 ? 'success' : 'info'"
                  size="small"
                  effect="plain"
                >
                  {{ routeDetail.status === 1 ? '启用中' : '已停用' }}
                </el-tag>
              </h3>
              <p class="detail-meta">
                {{ routeDetail.subjectIcon }} {{ routeDetail.subjectName }}
                <span>·</span>
                {{ routeDetail.targetEducation || '适合学历不限' }}
                <span>·</span>
                共 {{ routeDetail.courseCount }} 门课程
                <span>·</span>
                总计时长 {{ formatDuration(routeDetail.totalDurationMinutes) }}
              </p>
              <p class="detail-desc">{{ routeDetail.description || '暂无路线描述' }}</p>
            </div>
            <el-button
              v-permission="'route:edit'"
              type="primary"
              :icon="Plus"
              @click="openCoursePicker"
            >
              从课程库添加
            </el-button>
          </div>

          <el-empty
            v-if="!routeDetail.courses.length"
            description="暂无课程，点击右上角「从课程库添加」"
          />
          <el-table v-else :data="routeDetail.courses" class="detail-table">
            <el-table-column label="序号" width="60" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="课程" min-width="280">
              <template #default="{ row }">
                <div class="detail-course">
                  <img
                    v-if="row.coverUrl && !coverErrors[row.courseId]"
                    :src="courseCover(row.coverUrl)"
                    class="detail-cover"
                    referrerpolicy="no-referrer"
                    loading="lazy"
                    alt=""
                    @error="handleCoverError(row.courseId)"
                  />
                  <span v-else class="detail-cover detail-cover--empty">无</span>
                  <div class="detail-course-info">
                    <div class="detail-course-name">{{ row.name }}</div>
                    <div class="detail-course-sub">{{ row.bvid || '其他来源课程' }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="分组" width="100" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.localSubject ? 'success' : 'warning'"
                  size="small"
                  effect="plain"
                >
                  {{ row.localSubject ? '本学科' : '跨学科' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="subjectName" label="学科" width="150" />
            <el-table-column label="时长" width="120">
              <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
            </el-table-column>
            <el-table-column label="排序" width="90" align="center">
              <template #default="{ $index }">
                <div class="sort-actions">
                  <el-button
                    v-permission="'route:edit'"
                    text
                    circle
                    size="small"
                    :icon="Top"
                    :disabled="$index === 0"
                    @click="moveDetailCourse($index, -1)"
                  />
                  <el-button
                    v-permission="'route:edit'"
                    text
                    circle
                    size="small"
                    :icon="Bottom"
                    :disabled="$index === (routeDetail?.courses.length ?? 0) - 1"
                    @click="moveDetailCourse($index, 1)"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button
                  v-permission="'route:edit'"
                  link
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="removeDetailCourse(row.courseId, row.name)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="pickerVisible"
      title="从课程库添加课程"
      width="760px"
      align-center
      destroy-on-close
    >
      <div class="picker-toolbar">
        <el-input
          v-model="courseKeyword"
          placeholder="搜索课程名称 / BV号 / 学科"
          clearable
          :prefix-icon="Search"
          style="width: 300px"
        />
        <span>已选择 {{ selectedCourseIds.length }} 门</span>
      </div>

      <div v-loading="pickerLoading" class="picker-body">
        <el-checkbox-group v-model="selectedCourseIds">
          <template v-if="filteredLocalCourses.length">
            <div class="picker-group-title">本学科课程 <span>{{ filteredLocalCourses.length }}</span></div>
            <div v-for="course in filteredLocalCourses" :key="course.courseId" class="picker-item">
              <el-checkbox :value="course.courseId" />
              <img
                v-if="course.coverUrl && !coverErrors[course.courseId]"
                :src="courseCover(course.coverUrl)"
                class="picker-cover"
                referrerpolicy="no-referrer"
                loading="lazy"
                alt=""
                @error="handleCoverError(course.courseId)"
              />
              <span v-else class="picker-cover picker-cover--empty">无</span>
              <div class="picker-info">
                <div class="picker-name">{{ course.name }}</div>
                <div class="picker-meta">
                  {{ course.bvid || '其他来源' }} · {{ course.subjectName }} ·
                  {{ formatDuration(course.durationMinutes) }}
                </div>
              </div>
              <el-tag size="small" type="success" effect="plain">本学科</el-tag>
            </div>
          </template>

          <template v-if="filteredOtherCourses.length">
            <div class="picker-group-title">其他学科课程（跨学科补充） <span>{{ filteredOtherCourses.length }}</span></div>
            <div v-for="course in filteredOtherCourses" :key="course.courseId" class="picker-item">
              <el-checkbox :value="course.courseId" />
              <img
                v-if="course.coverUrl && !coverErrors[course.courseId]"
                :src="courseCover(course.coverUrl)"
                class="picker-cover"
                referrerpolicy="no-referrer"
                loading="lazy"
                alt=""
                @error="handleCoverError(course.courseId)"
              />
              <span v-else class="picker-cover picker-cover--empty">无</span>
              <div class="picker-info">
                <div class="picker-name">{{ course.name }}</div>
                <div class="picker-meta">
                  {{ course.bvid || '其他来源' }} · {{ course.subjectName }} ·
                  {{ formatDuration(course.durationMinutes) }}
                </div>
              </div>
              <el-tag size="small" type="warning" effect="plain">跨学科</el-tag>
            </div>
          </template>
        </el-checkbox-group>

        <el-empty
          v-if="!pickerLoading && !filteredLocalCourses.length && !filteredOtherCourses.length"
          description="没有可添加的课程"
        />
      </div>

      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button
          type="primary"
          :icon="Check"
          :disabled="selectedCourseIds.length === 0"
          :loading="pickerSubmitting"
          @click="confirmAddCourses"
        >
          添加课程{{ selectedCourseIds.length ? ` (${selectedCourseIds.length})` : '' }}
        </el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.route-page {
  min-width: 0;
}

.filter-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px 18px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
}

.filter-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.route-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
  min-height: 180px;
}

.route-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 18px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.route-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ys-shadow-md);
}

.route-card__top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.route-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(124, 108, 240, 0.12);
  font-size: 22px;
}

.route-title-wrap {
  flex: 1;
  min-width: 0;
}

.route-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.route-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ys-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-subtitle {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.route-card__actions {
  display: flex;
  gap: 0;
  flex: none;
}

.route-description {
  min-height: 38px;
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ys-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.route-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.route-meta strong {
  color: var(--ys-text-primary);
  font-weight: 600;
}

.route-preview-list {
  flex: 1;
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: var(--ys-radius-md);
  background: #f8f9fc;
}

.route-preview-empty {
  padding: 6px 2px;
  font-size: 12px;
  color: var(--ys-text-placeholder);
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
  font-size: 12px;
}

.preview-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: #fff;
  color: var(--ys-text-secondary);
  font-size: 10px;
}

.preview-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ys-text-primary);
}

.preview-duration {
  flex: none;
  color: var(--ys-text-secondary);
  font-size: 11px;
}

.preview-more {
  margin-top: 4px;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.route-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--ys-border-light);
}

.route-status-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.route-empty {
  grid-column: 1 / -1;
  padding: 40px 0;
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

.detail-body {
  min-height: 180px;
}

.detail-hero {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: var(--ys-radius-md);
  background: #f8f9fc;
}

.detail-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #fff;
  font-size: 25px;
}

.detail-main {
  flex: 1;
  min-width: 0;
}

.detail-main h3 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  font-size: 17px;
  color: var(--ys-text-primary);
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.detail-desc {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ys-text-secondary);
}

.detail-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.detail-course {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-cover {
  display: block;
  flex: none;
  width: 72px;
  height: 42px;
  border-radius: 5px;
  object-fit: cover;
  background: #f5f7fa;
}

.detail-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.detail-course-info {
  min-width: 0;
}

.detail-course-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--ys-text-primary);
}

.detail-course-sub {
  margin-top: 3px;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

.sort-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.picker-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.picker-body {
  min-height: 160px;
  max-height: 48vh;
  overflow-y: auto;
  padding-right: 4px;
}

.picker-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.picker-group-title span {
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--ys-bg);
  color: var(--ys-text-secondary);
  font-size: 11px;
  font-weight: 500;
}

.picker-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: var(--ys-radius-sm);
  font-size: 12px;
  line-height: 1.5;
  transition: background 0.2s ease;
}

.picker-item:hover {
  background: var(--ys-bg);
}

.picker-item :deep(.el-checkbox) {
  flex: none;
  margin-right: 0;
}

.picker-cover {
  display: block;
  flex: none;
  width: 58px;
  height: 34px;
  border-radius: 4px;
  object-fit: cover;
  background: #f5f7fa;
}

.picker-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: var(--ys-text-secondary);
}

.picker-info {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.5;
}

.picker-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--ys-text-primary);
}

.picker-meta {
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--ys-text-secondary);
}

@media (max-width: 1280px) {
  .route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .route-grid {
    grid-template-columns: 1fr;
  }

  .filter-count {
    width: 100%;
    margin-left: 0;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }

  .detail-hero {
    flex-wrap: wrap;
  }
}
</style>
