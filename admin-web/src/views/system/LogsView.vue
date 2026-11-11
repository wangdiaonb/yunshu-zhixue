<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import { clearLogs, exportLogs, getLog, pageLogs } from '@/api/system/log'
import type { OperationLogItem } from '@/types/system'

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const loading = ref(false)
const exporting = ref(false)
const logs = ref<OperationLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const dateRange = ref<string[] | null>(null)

const filters = reactive<{ keyword: string; operationType?: string }>({
  keyword: '',
  operationType: undefined
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<OperationLogItem | null>(null)

const actionOptions = ['登录', '登出', '新增', '修改', '删除', '禁用启用', '导出', '其他']

onMounted(loadLogs)

function buildQuery() {
  return {
    keyword: filters.keyword.trim() || undefined,
    operationType: filters.operationType || undefined,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1]
  }
}

async function loadLogs(): Promise<void> {
  loading.value = true
  try {
    const data = await pageLogs({
      ...buildQuery(),
      page: page.value,
      pageSize: pageSize.value
    })
    logs.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void loadLogs()
}

function resetFilters(): void {
  filters.keyword = ''
  filters.operationType = undefined
  dateRange.value = null
  handleSearch()
}

function handleSizeChange(size: number): void {
  pageSize.value = size
  page.value = 1
  void loadLogs()
}

async function openDetail(row: OperationLogItem): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getLog(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function handleExport(): Promise<void> {
  exporting.value = true
  try {
    const blob = await exportLogs(buildQuery())
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'operation-logs.csv'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('日志导出已开始')
  } finally {
    exporting.value = false
  }
}

async function handleClear(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要清空所有操作日志吗？此操作不可恢复！', '清空日志', {
      type: 'warning',
      confirmButtonText: '确定清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await clearLogs()
  ElMessage.success('操作日志已清空')
  page.value = 1
  await loadLogs()
}

function actionTagType(action: string): TagType {
  switch (action) {
    case '登录':
      return 'success'
    case '登出':
      return 'info'
    case '新增':
      return 'primary'
    case '修改':
      return 'warning'
    case '删除':
      return 'danger'
    case '禁用启用':
      return 'warning'
    case '导出':
      return 'info'
    default:
      return 'info'
  }
}
</script>

<template>
  <PageShell title="操作日志" description="记录管理端登录登出、写操作与数据导出，支持筛选、查看详情与 CSV 导出。">
    <template #actions>
      <el-button v-permission="'system:log_view'" :icon="Download" :loading="exporting" @click="handleExport">
        导出日志
      </el-button>
      <el-button v-permission="'system:log_view'" type="danger" plain :icon="Delete" @click="handleClear">
        清空日志
      </el-button>
    </template>

    <template #toolbar>
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索用户/操作/目标"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.operationType" placeholder="全部操作" clearable style="width: 150px">
          <el-option v-for="action in actionOptions" :key="action" :label="action" :value="action" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
      </div>
    </template>

    <el-table
      v-loading="loading"
      :data="logs"
      row-key="id"
      empty-text="暂无匹配的日志记录"
      class="log-table"
      @row-click="openDetail"
    >
      <el-table-column label="用户" width="150">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="28">{{ row.username.slice(0, 1).toUpperCase() }}</el-avatar>
            <span>{{ row.username }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.action)" effect="light">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标" min-width="220">
        <template #default="{ row }">
          <div class="target-cell">
            <span>{{ row.targetName }}</span>
            <span class="muted">{{ row.targetType }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP 地址" width="150" />
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ row.createdAt || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" :icon="View" @click.stop="openDetail(row)">详情</el-button>
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
        @current-change="loadLogs"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="detailVisible" title="日志详情" width="560px">
      <el-skeleton v-if="detailLoading" :rows="5" animated />
      <el-descriptions v-else-if="detail" :column="1" border>
        <el-descriptions-item label="日志 ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="actionTagType(detail.action)" effect="light">{{ detail.action }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作对象">
          {{ detail.targetName }}（{{ detail.targetType }}）
        </el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.createdAt || '—' }}</el-descriptions-item>
      </el-descriptions>
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

.log-table :deep(.el-table__row) {
  cursor: pointer;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.target-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.target-cell .muted {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
