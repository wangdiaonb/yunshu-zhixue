<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  CircleClose,
  Delete,
  Refresh,
  RefreshLeft,
  Search,
  View
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  batchChangeStudentStatus,
  batchDeleteStudents,
  changeStudentStatus,
  deleteStudent,
  getStudentDetail,
  pageStudentInterviews,
  pageStudents
} from '@/api/student'
import type { StudentDetail, StudentInterviewRecord, StudentItem } from '@/types/student'

const loading = ref(false)
const students = ref<StudentItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selection = ref<StudentItem[]>([])
const tableRef = ref<{ clearSelection: () => void }>()

const filters = reactive<{ keyword: string; status?: number }>({
  keyword: '',
  status: undefined
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<StudentDetail | null>(null)

const recordLoading = ref(false)
const records = ref<StudentInterviewRecord[]>([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordPageSize = 5

onMounted(() => {
  void loadStudents().catch(() => undefined)
})

async function loadStudents(): Promise<void> {
  loading.value = true
  try {
    const data = await pageStudents({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
      page: page.value,
      pageSize: pageSize.value
    })
    students.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void loadStudents().catch(() => undefined)
}

function resetFilters(): void {
  filters.keyword = ''
  filters.status = undefined
  handleSearch()
}

function handleSizeChange(size: number): void {
  pageSize.value = size
  page.value = 1
  void loadStudents().catch(() => undefined)
}

function handleSelectionChange(rows: StudentItem[]): void {
  selection.value = rows
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

async function toggleStatus(row: StudentItem | StudentDetail): Promise<void> {
  const nextStatus = row.status === 1 ? 0 : 1
  const label = nextStatus === 1 ? '启用' : '禁用'
  const tip =
    nextStatus === 1
      ? `确认启用学员「${row.nickname}」吗？启用后可正常登录前台。`
      : `确认禁用学员「${row.nickname}」吗？禁用后该学员无法登录前台，已有登录态在令牌过期后失效。`
  if (!(await confirmAction(tip, `${label}学员`))) {
    return
  }
  await changeStudentStatus(row.id, nextStatus)
  ElMessage.success(`学员已${label}`)
  await loadStudents()
  if (detailVisible.value && detail.value?.id === row.id) {
    detail.value = await getStudentDetail(row.id)
  }
}

async function handleDelete(row: StudentItem | StudentDetail): Promise<void> {
  if (!(await confirmAction(`确认删除学员「${row.nickname}」吗？此操作不可恢复。`, '删除学员'))) {
    return
  }
  await deleteStudent(row.id)
  ElMessage.success('学员已删除')
  detailVisible.value = false
  await loadStudents()
}

async function handleBatchStatus(status: number): Promise<void> {
  if (!selection.value.length) {
    return
  }
  const label = status === 1 ? '启用' : '禁用'
  const tip =
    status === 1
      ? `确认启用选中的 ${selection.value.length} 个学员吗？`
      : `确认禁用选中的 ${selection.value.length} 个学员吗？禁用后这些学员无法登录前台。`
  if (!(await confirmAction(tip, `批量${label}`))) {
    return
  }
  const affected = await batchChangeStudentStatus(
    selection.value.map((item) => item.id),
    status
  )
  ElMessage.success(`已${label} ${affected} 个学员`)
  await loadStudents()
}

async function handleBatchDelete(): Promise<void> {
  if (!selection.value.length) {
    return
  }
  if (
    !(await confirmAction(
      `确认删除选中的 ${selection.value.length} 个学员吗？此操作不可恢复。`,
      '批量删除学员'
    ))
  ) {
    return
  }
  const affected = await batchDeleteStudents(selection.value.map((item) => item.id))
  ElMessage.success(`已删除 ${affected} 个学员`)
  tableRef.value?.clearSelection()
  await loadStudents()
}

async function openDetail(row: StudentItem): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  records.value = []
  recordTotal.value = 0
  recordPage.value = 1
  try {
    detail.value = await getStudentDetail(row.id)
    await loadRecords(row.id, 1)
  } finally {
    detailLoading.value = false
  }
}

async function loadRecords(studentId: number, targetPage: number): Promise<void> {
  recordLoading.value = true
  try {
    const data = await pageStudentInterviews(studentId, targetPage, recordPageSize)
    records.value = data.list
    recordTotal.value = data.total
    recordPage.value = targetPage
  } finally {
    recordLoading.value = false
  }
}

function handleRecordPageChange(targetPage: number): void {
  if (!detail.value) {
    return
  }
  void loadRecords(detail.value.id, targetPage).catch(() => undefined)
}

function formatDateTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

function difficultyLabel(difficulty: number): string {
  return { 1: '简单', 2: '中等', 3: '困难' }[difficulty] ?? '—'
}

function difficultyTagType(difficulty: number): 'success' | 'warning' | 'danger' {
  return ({ 1: 'success', 2: 'warning', 3: 'danger' } as const)[difficulty] ?? 'warning'
}

function formatScore(score: number | null): string {
  return score === null || score === undefined ? '—' : score.toFixed(1)
}
</script>

<template>
  <PageShell
    title="学员管理"
    description="管理前台注册学员：查看学习与面试数据、启用/禁用、删除。学员由前台注册产生，后台不提供新增与编辑。"
  >
    <template #toolbar>
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索昵称 / 手机号"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="已启用" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
      </div>
    </template>

    <div class="table-toolbar">
      <span class="selection-text">
        {{ selection.length ? `已选择 ${selection.length} 个学员` : '' }}
      </span>
      <div class="table-actions">
        <el-button
          v-permission="'student:edit'"
          size="small"
          :icon="CircleCheck"
          :disabled="!selection.length"
          @click="handleBatchStatus(1)"
        >
          批量启用
        </el-button>
        <el-button
          v-permission="'student:edit'"
          size="small"
          :icon="CircleClose"
          :disabled="!selection.length"
          @click="handleBatchStatus(0)"
        >
          批量禁用
        </el-button>
        <el-button
          v-permission="'student:delete'"
          size="small"
          type="danger"
          plain
          :icon="Delete"
          :disabled="!selection.length"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
        <el-button size="small" :icon="Refresh" @click="loadStudents">刷新</el-button>
      </div>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="students"
      row-key="id"
      empty-text="没有找到匹配的学员"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" reserve-selection />
      <el-table-column label="学员" min-width="190">
        <template #default="{ row }">
          <div class="student-cell">
            <el-avatar :size="36" :src="row.avatar || undefined">
              {{ row.nickname.slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div class="student-meta">
              <el-button link type="primary" class="student-name" @click="openDetail(row)">
                {{ row.nickname }}
              </el-button>
              <span class="student-email">{{ row.email || '未填写邮箱' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column label="注册时间" width="150">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="学习天数" width="100" align="center">
        <template #default="{ row }">{{ row.studyDays }} 天</template>
      </el-table-column>
      <el-table-column label="刷题 / 面试" width="140" align="center">
        <template #default="{ row }">
          {{ row.practiceCount }} 题 / {{ row.interviewCount }} 次
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '已启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" size="small" :icon="View" @click="openDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-permission="'student:edit'"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              :icon="row.status === 1 ? CircleClose : CircleCheck"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-permission="'student:delete'"
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
        @current-change="loadStudents"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="detailVisible"
      title="学员详情"
      width="900px"
      align-center
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-grid">
            <div class="detail-profile">
              <el-avatar :size="64" :src="detail.avatar || undefined" class="detail-avatar">
                {{ detail.nickname.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <h3>{{ detail.nickname }}</h3>
              <el-tag :type="detail.status === 1 ? 'success' : 'info'" effect="light">
                {{ detail.status === 1 ? '已启用' : '已禁用' }}
              </el-tag>
              <ul class="detail-info">
                <li>
                  <span class="label">手机号</span>
                  <span class="value">{{ detail.mobile }}</span>
                </li>
                <li>
                  <span class="label">邮箱</span>
                  <span class="value">{{ detail.email || '—' }}</span>
                </li>
                <li>
                  <span class="label">注册时间</span>
                  <span class="value">{{ formatDateTime(detail.createdAt) }}</span>
                </li>
                <li>
                  <span class="label">最后登录</span>
                  <span class="value">{{ formatDateTime(detail.lastLoginAt) }}</span>
                </li>
              </ul>
            </div>

            <div class="detail-main">
              <h4 class="section-title">学习统计</h4>
              <div class="stats-row">
                <div class="stat-item">
                  <div class="num blue">{{ detail.studyDays }}</div>
                  <div class="desc">学习天数</div>
                </div>
                <div class="stat-item">
                  <div class="num green">{{ detail.courseCount }}</div>
                  <div class="desc">已学课程</div>
                </div>
                <div class="stat-item">
                  <div class="num orange">{{ detail.practiceCount }}</div>
                  <div class="desc">刷题总数</div>
                </div>
                <div class="stat-item">
                  <div class="num purple">{{ detail.accuracy }}%</div>
                  <div class="desc">正确率</div>
                </div>
              </div>

              <h4 class="section-title">
                面试记录
                <span class="section-extra">累计 {{ detail.interviewCount }} 次</span>
              </h4>
              <el-table
                v-loading="recordLoading"
                :data="records"
                size="small"
                empty-text="暂无面试记录"
                max-height="260"
              >
                <el-table-column label="方向" min-width="150" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.subjectName || '—' }}</template>
                </el-table-column>
                <el-table-column label="难度" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="difficultyTagType(row.difficulty)" size="small" effect="plain">
                      {{ difficultyLabel(row.difficulty) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="题数" width="70" align="center">
                  <template #default="{ row }">{{ row.questionCount }}</template>
                </el-table-column>
                <el-table-column label="评分" width="80" align="center">
                  <template #default="{ row }">{{ formatScore(row.overallScore) }}</template>
                </el-table-column>
                <el-table-column label="完成时间" width="140">
                  <template #default="{ row }">{{ formatDateTime(row.completedAt) }}</template>
                </el-table-column>
              </el-table>
              <div v-if="recordTotal > recordPageSize" class="record-pagination">
                <el-pagination
                  :current-page="recordPage"
                  :page-size="recordPageSize"
                  :total="recordTotal"
                  layout="prev, pager, next"
                  small
                  @current-change="handleRecordPageChange"
                />
              </div>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <template v-if="detail">
          <el-button
            v-permission="'student:edit'"
            :type="detail.status === 1 ? 'warning' : 'success'"
            plain
            :icon="detail.status === 1 ? CircleClose : CircleCheck"
            @click="toggleStatus(detail)"
          >
            {{ detail.status === 1 ? '禁用学员' : '启用学员' }}
          </el-button>
          <el-button
            v-permission="'student:delete'"
            type="danger"
            plain
            :icon="Delete"
            @click="handleDelete(detail)"
          >
            删除
          </el-button>
        </template>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.selection-text {
  font-size: 13px;
  color: var(--ys-text-secondary);
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.student-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.4;
}

.student-name {
  padding: 0;
  justify-content: flex-start;
  font-weight: 600;
}

.student-email {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

.detail-profile {
  text-align: center;
  padding-right: 24px;
  border-right: 1px solid var(--ys-border-light);
}

.detail-avatar {
  margin-bottom: 12px;
  font-size: 24px;
}

.detail-profile h3 {
  margin-bottom: 8px;
  font-size: 18px;
  font-weight: 600;
}

.detail-info {
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
  text-align: left;
}

.detail-info li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--ys-border-light);
  font-size: 13px;
}

.detail-info .label {
  color: var(--ys-text-secondary);
}

.detail-info .value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: var(--ys-text-primary);
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.detail-main .section-title:not(:first-child) {
  margin-top: 20px;
}

.section-extra {
  font-size: 12px;
  font-weight: 400;
  color: var(--ys-text-secondary);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-item {
  padding: 12px;
  border-radius: var(--ys-radius-md);
  background: var(--ys-bg);
  text-align: center;
}

.stat-item .num {
  font-size: 20px;
  font-weight: 700;
}

.stat-item .num.blue {
  color: #409eff;
}

.stat-item .num.green {
  color: #67c23a;
}

.stat-item .num.orange {
  color: #e6a23c;
}

.stat-item .num.purple {
  color: #9b59b6;
}

.stat-item .desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.record-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-profile {
    padding-right: 0;
    padding-bottom: 16px;
    border-right: none;
    border-bottom: 1px solid var(--ys-border-light);
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
