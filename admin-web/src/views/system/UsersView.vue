<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  CircleCheck,
  CircleClose,
  Delete,
  Edit,
  Lock,
  Plus,
  Refresh,
  RefreshLeft,
  Search,
  Unlock
} from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  batchDeleteUsers,
  batchUpdateUserStatus,
  createUser,
  deleteUser,
  getUserDetail,
  listRoleOptions,
  pageUsers,
  resetUserPassword,
  updateUser
} from '@/api/system/user'
import type { AdminUserDetail, AdminUserItem, RoleOption } from '@/types/system'

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'
type DialogMode = 'create' | 'edit'

const loading = ref(false)
const users = ref<AdminUserItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selection = ref<AdminUserItem[]>([])
const roleOptions = ref<RoleOption[]>([])

const filters = reactive<{ keyword: string; roleId?: number | string; status?: number | string }>({
  keyword: '',
  roleId: undefined,
  status: undefined
})

const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('create')
const editingId = ref<number | null>(null)
const editingBuiltin = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  mobile: '',
  email: '',
  roleId: undefined as number | undefined,
  password: '',
  status: 1
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminUserDetail | null>(null)

const assignableRoles = computed(() =>
  roleOptions.value.filter((role) => role.code !== 'super_admin' || editingBuiltin.value)
)

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { max: 50, message: '用户名长度不能超过 50 个字符', trigger: 'blur' }
  ],
  mobile: [
    {
      validator: (_rule, value: string, callback) => {
        const mobile = (value ?? '').trim()
        if (!mobile && editingBuiltin.value) {
          callback()
          return
        }
        if (!mobile) {
          callback(new Error('请输入手机号'))
          return
        }
        if (!/^1[3-9]\d{9}$/.test(mobile)) {
          callback(new Error('手机号格式不正确'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          callback()
          return
        }
        callback(new Error('邮箱格式不正确'))
      },
      trigger: 'blur'
    }
  ],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        const password = value ?? ''
        if (dialogMode.value === 'create' && !password) {
          callback(new Error('请输入密码'))
          return
        }
        if (password && (password.length < 6 || password.length > 20)) {
          callback(new Error('密码长度为 6~20 位'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  await Promise.all([loadRoleOptions(), loadUsers()])
})

async function loadRoleOptions(): Promise<void> {
  roleOptions.value = await listRoleOptions()
}

async function loadUsers(): Promise<void> {
  loading.value = true
  try {
    const data = await pageUsers({
      keyword: filters.keyword.trim() || undefined,
      roleId: filters.roleId ? Number(filters.roleId) : undefined,
      status: filters.status === 0 || filters.status === 1 ? filters.status : undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    users.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void loadUsers()
}

function resetFilters(): void {
  filters.keyword = ''
  filters.roleId = undefined
  filters.status = undefined
  handleSearch()
}

function handleSizeChange(size: number): void {
  pageSize.value = size
  page.value = 1
  void loadUsers()
}

function handleSelectionChange(rows: AdminUserItem[]): void {
  selection.value = rows
}

function openCreate(): void {
  dialogMode.value = 'create'
  editingId.value = null
  editingBuiltin.value = false
  Object.assign(form, {
    username: '',
    mobile: '',
    email: '',
    roleId: undefined,
    password: '',
    status: 1
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: AdminUserItem): void {
  dialogMode.value = 'edit'
  editingId.value = row.id
  editingBuiltin.value = row.isBuiltin === 1
  Object.assign(form, {
    username: row.username,
    mobile: row.mobile ?? '',
    email: row.email ?? '',
    roleId: row.roleId,
    password: '',
    status: row.status
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function submitForm(): Promise<void> {
  const formInstance = formRef.value
  if (!formInstance) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      username: form.username.trim(),
      mobile: form.mobile.trim(),
      email: form.email.trim() || null,
      roleId: form.roleId as number,
      status: form.status
    }
    if (dialogMode.value === 'create') {
      await createUser({ ...payload, password: form.password })
      ElMessage.success('用户创建成功')
      page.value = 1
    } else if (editingId.value) {
      await updateUser(editingId.value, { ...payload, password: form.password || undefined })
      ElMessage.success('用户信息已更新')
    }
    dialogVisible.value = false
    await loadUsers()
  } finally {
    submitting.value = false
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

async function toggleStatus(row: AdminUserItem): Promise<void> {
  if (row.isBuiltin === 1) {
    ElMessage.warning('内置超级管理员不可禁用')
    return
  }
  const nextStatus = row.status === 1 ? 0 : 1
  const label = nextStatus === 1 ? '启用' : '禁用'
  if (!(await confirmAction(`确认${label}用户「${row.username}」吗？`))) {
    return
  }
  await batchUpdateUserStatus([row.id], nextStatus)
  ElMessage.success(`用户已${label}`)
  await loadUsers()
}

async function handleDelete(row: AdminUserItem): Promise<void> {
  if (row.isBuiltin === 1) {
    ElMessage.warning('内置超级管理员不可删除')
    return
  }
  if (!(await confirmAction(`确认删除用户「${row.username}」吗？此操作不可恢复。`, '删除用户'))) {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('用户已删除')
  await loadUsers()
}

async function handleReset(row: AdminUserItem): Promise<void> {
  if (!(await confirmAction(`确认将用户「${row.username}」的密码重置为默认密码吗？`, '重置密码'))) {
    return
  }
  const result = await resetUserPassword(row.id)
  await ElMessageBox.alert(`新密码：${result.password}`, '密码已重置', {
    confirmButtonText: '我知道了'
  })
}

async function handleBatchStatus(status: number): Promise<void> {
  if (!selection.value.length) {
    return
  }
  if (status === 0 && selection.value.some((item) => item.isBuiltin === 1)) {
    ElMessage.warning('内置超级管理员不可禁用')
    return
  }
  const label = status === 1 ? '启用' : '禁用'
  if (!(await confirmAction(`确认批量${label} ${selection.value.length} 个用户吗？`))) {
    return
  }
  await batchUpdateUserStatus(selection.value.map((item) => item.id), status)
  ElMessage.success(`已批量${label}`)
  await loadUsers()
}

async function handleBatchDelete(): Promise<void> {
  if (!selection.value.length) {
    return
  }
  if (selection.value.some((item) => item.isBuiltin === 1)) {
    ElMessage.warning('内置超级管理员不可删除')
    return
  }
  if (!(await confirmAction(`确认批量删除 ${selection.value.length} 个用户吗？此操作不可恢复。`, '批量删除'))) {
    return
  }
  await batchDeleteUsers(selection.value.map((item) => item.id))
  ElMessage.success('已批量删除')
  await loadUsers()
}

async function openDetail(row: AdminUserItem): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getUserDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

function roleTagType(code?: string | null): TagType {
  if (code === 'super_admin') {
    return 'danger'
  }
  if (code === 'content_reviewer') {
    return 'warning'
  }
  if (code === 'operator') {
    return 'success'
  }
  return 'primary'
}
</script>

<template>
  <PageShell title="用户管理" description="维护管理后台内部账号：新增、编辑、启停、重置密码与批量操作。">
    <template #actions>
      <el-button v-permission="'user:add'" type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </template>

    <template #toolbar>
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索用户名/手机号"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.roleId" placeholder="全部角色" clearable style="width: 160px">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="已启用" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
      </div>
    </template>

    <div class="table-toolbar">
      <span class="selection-text">{{ selection.length ? `已选择 ${selection.length} 项` : '' }}</span>
      <div class="table-actions">
        <el-button
          v-permission="'user:edit'"
          size="small"
          :icon="CircleCheck"
          :disabled="!selection.length"
          @click="handleBatchStatus(1)"
        >
          批量启用
        </el-button>
        <el-button
          v-permission="'user:edit'"
          size="small"
          :icon="CircleClose"
          :disabled="!selection.length"
          @click="handleBatchStatus(0)"
        >
          批量禁用
        </el-button>
        <el-button
          v-permission="'user:disable'"
          size="small"
          type="danger"
          plain
          :icon="Delete"
          :disabled="!selection.length"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
        <el-button size="small" :icon="Refresh" @click="loadUsers">刷新</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="users"
      row-key="id"
      empty-text="没有找到匹配的用户"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column label="用户" min-width="200">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="34">{{ row.username.slice(0, 1).toUpperCase() }}</el-avatar>
            <div class="user-meta">
              <el-button link type="primary" class="user-name" @click="openDetail(row)">
                {{ row.username }}
              </el-button>
              <el-tag v-if="row.isBuiltin === 1" type="danger" size="small" effect="plain">内置</el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">{{ row.mobile || '—' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="130">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.roleCode)" effect="light">{{ row.roleName || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '已启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="170">
        <template #default="{ row }">{{ row.lastLoginAt || '从未登录' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
          <el-button
            v-permission="'user:edit'"
            link
            type="primary"
            size="small"
            :icon="Edit"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-permission="'user:edit'"
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            size="small"
            :icon="row.status === 1 ? Lock : Unlock"
            :disabled="row.isBuiltin === 1"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            v-permission="'user:edit'"
            link
            type="primary"
            size="small"
            :icon="Refresh"
            @click="handleReset(row)"
          >
            重置密码
          </el-button>
          <el-button
            v-permission="'user:disable'"
            link
            type="danger"
            size="small"
            :icon="Delete"
            :disabled="row.isBuiltin === 1"
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
        @current-change="loadUsers"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" maxlength="50" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="手机号" prop="mobile">
            <el-input
              v-model="form.mobile"
              maxlength="11"
              :placeholder="editingBuiltin ? '内置账号可留空' : '请输入手机号'"
            />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="100" placeholder="选填" />
          </el-form-item>
          <el-form-item label="角色" prop="roleId">
            <el-select
              v-model="form.roleId"
              placeholder="请选择角色"
              :disabled="editingBuiltin"
              style="width: 100%"
            >
              <el-option v-for="role in assignableRoles" :key="role.id" :label="role.name" :value="role.id" />
            </el-select>
          </el-form-item>
          <el-form-item :label="dialogMode === 'create' ? '密码' : '新密码'" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="dialogMode === 'create' ? '6~20 位密码' : '留空则不修改密码'"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="editingBuiltin"
              active-text="已启用"
              inactive-text="已禁用"
            />
          </el-form-item>
        </div>
        <p v-if="editingBuiltin" class="form-tip">内置超级管理员的角色与状态不可修改。</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用户详情" width="720px">
      <el-skeleton v-if="detailLoading" :rows="6" animated />
      <template v-else-if="detail">
        <div class="detail-head">
          <el-avatar :size="56">{{ detail.user.username.slice(0, 1).toUpperCase() }}</el-avatar>
          <div>
            <h3>{{ detail.user.username }}</h3>
            <div class="detail-tags">
              <el-tag :type="roleTagType(detail.user.roleCode)" effect="light">
                {{ detail.user.roleName || '—' }}
              </el-tag>
              <el-tag :type="detail.user.status === 1 ? 'success' : 'info'" effect="light">
                {{ detail.user.status === 1 ? '已启用' : '已禁用' }}
              </el-tag>
              <el-tag v-if="detail.user.isBuiltin === 1" type="danger" effect="plain">内置</el-tag>
            </div>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="手机号">{{ detail.user.mobile || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.user.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.user.createdAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ detail.user.lastLoginAt || '从未登录' }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="recent-title">最近操作</h4>
        <el-empty v-if="!detail.recentLogs.length" description="暂无操作记录" :image-size="80" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="log in detail.recentLogs"
            :key="log.id"
            :timestamp="log.createdAt || ''"
            placement="top"
          >
            {{ log.action }} · {{ log.targetName }}
          </el-timeline-item>
        </el-timeline>
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  padding: 0;
  font-weight: 600;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.form-tip {
  margin: 0;
  font-size: 12px;
  color: var(--ys-warning);
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-head h3 {
  font-size: 18px;
  font-weight: 600;
}

.detail-tags {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.recent-title {
  margin: 22px 0 12px;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
