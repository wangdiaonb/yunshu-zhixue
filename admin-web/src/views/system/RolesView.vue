<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import { Check, Delete, Plus, Setting } from '@element-plus/icons-vue'
import PageShell from '@/components/PageShell.vue'
import {
  createRole,
  deleteRole,
  getPermissionTree,
  listRoles,
  saveRolePermissions
} from '@/api/system/role'
import type { PermissionNode, RoleCard } from '@/types/system'

const loading = ref(false)
const roles = ref<RoleCard[]>([])

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ name: '', code: '', description: '' })

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称不能超过 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { max: 50, message: '角色编码不能超过 50 个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
      message: '角色编码需以字母开头，仅包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  description: [{ max: 255, message: '角色描述不能超过 255 个字符', trigger: 'blur' }]
}

const permissionVisible = ref(false)
const permissionLoading = ref(false)
const savingPermissions = ref(false)
const treeRef = ref<InstanceType<typeof ElTree>>()
const permissionTree = ref<PermissionNode[]>([])
const currentRole = ref<RoleCard | null>(null)

const roleCountText = computed(() => `共 ${roles.value.length} 个角色`)

onMounted(loadRoles)

async function loadRoles(): Promise<void> {
  loading.value = true
  try {
    roles.value = await listRoles()
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  Object.assign(createForm, { name: '', code: '', description: '' })
  createVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

async function submitCreate(): Promise<void> {
  const formInstance = createFormRef.value
  if (!formInstance) {
    return
  }
  const valid = await formInstance.validate().catch(() => false)
  if (!valid) {
    return
  }
  creating.value = true
  try {
    await createRole({
      name: createForm.name.trim(),
      code: createForm.code.trim(),
      description: createForm.description.trim() || undefined
    })
    ElMessage.success('角色创建成功')
    createVisible.value = false
    await loadRoles()
  } finally {
    creating.value = false
  }
}

async function handleDelete(role: RoleCard): Promise<void> {
  if (role.superAdmin) {
    ElMessage.warning('超级管理员角色不可删除')
    return
  }
  if (role.userCount > 0) {
    ElMessage.warning(`该角色下有 ${role.userCount} 个用户，无法删除`)
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除角色「${role.name}」吗？`, '删除角色', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteRole(role.id)
  ElMessage.success('角色已删除')
  await loadRoles()
}

async function openPermissions(role: RoleCard): Promise<void> {
  if (role.superAdmin) {
    ElMessage.info('超级管理员拥有全部权限，不可编辑')
    return
  }
  currentRole.value = role
  permissionVisible.value = true
  permissionLoading.value = true
  permissionTree.value = []
  try {
    const tree = await getPermissionTree(role.id)
    permissionTree.value = tree
    await nextTick()
    const checkedKeys: number[] = []
    tree.forEach((group) => {
      group.children.forEach((child) => {
        if (child.checked) {
          checkedKeys.push(child.id)
        }
      })
    })
    treeRef.value?.setCheckedKeys(checkedKeys)
  } finally {
    permissionLoading.value = false
  }
}

async function savePermissions(): Promise<void> {
  if (!currentRole.value || !treeRef.value) {
    return
  }
  savingPermissions.value = true
  try {
    const checkedKeys = treeRef.value.getCheckedKeys(true) as number[]
    await saveRolePermissions(currentRole.value.id, checkedKeys)
    ElMessage.success('权限已保存')
    permissionVisible.value = false
    await loadRoles()
  } finally {
    savingPermissions.value = false
  }
}

function visiblePermissions(role: RoleCard) {
  return role.permissions.slice(0, 8)
}

function isAllPermissions(role: RoleCard): boolean {
  return role.superAdmin
    || (role.totalPermissionCount > 0 && role.permissions.length >= role.totalPermissionCount)
}
</script>

<template>
  <PageShell
    title="角色管理"
    description="预置超级管理员、内容审核、运营人员三个角色；权限保存为全量覆盖。本页仅超级管理员可访问。"
    :card="false"
  >
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
    </template>

    <div class="role-summary">{{ roleCountText }}</div>

    <div v-loading="loading" class="role-grid">
      <article
        v-for="role in roles"
        :key="role.id"
        class="role-card"
        :class="{ 'is-super': role.superAdmin }"
        @click="openPermissions(role)"
      >
        <header class="role-card__header">
          <div class="role-heading">
            <h3>{{ role.name }}</h3>
            <span class="role-code">{{ role.code }}</span>
          </div>
          <el-tag type="info" effect="plain">{{ role.userCount }} 用户</el-tag>
        </header>

        <p class="role-desc">{{ role.description || '暂无描述' }}</p>

        <div class="perm-tags">
          <el-tag v-if="isAllPermissions(role)" type="danger" effect="light">全部权限</el-tag>
          <template v-else-if="role.permissions.length">
            <el-tag
              v-for="permission in visiblePermissions(role)"
              :key="permission.id"
              size="small"
              effect="plain"
            >
              {{ permission.groupName }}·{{ permission.name }}
            </el-tag>
            <el-tag v-if="role.permissions.length > 8" size="small" type="info" effect="plain">
              +{{ role.permissions.length - 8 }}
            </el-tag>
          </template>
          <span v-else class="muted">暂无权限</span>
        </div>

        <footer class="role-actions" @click.stop>
          <el-button
            link
            type="primary"
            :icon="Setting"
            :disabled="role.superAdmin"
            @click="openPermissions(role)"
          >
            编辑权限
          </el-button>
          <el-button
            link
            type="danger"
            :icon="Delete"
            :disabled="role.superAdmin || role.userCount > 0"
            @click="handleDelete(role)"
          >
            删除
          </el-button>
          <span v-if="role.userCount > 0" class="muted action-hint">该角色下有用户，不可删除</span>
        </footer>
      </article>
    </div>

    <el-empty
      v-if="!loading && !roles.length"
      description="暂无角色数据，点击右上角新增角色"
    />

    <el-dialog v-model="createVisible" title="新增角色" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="createForm.name" maxlength="50" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="createForm.code" maxlength="50" placeholder="如：editor" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请输入角色描述（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="creating" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="permissionVisible"
      :title="`编辑权限 - ${currentRole?.name ?? ''}`"
      width="640px"
      destroy-on-close
    >
      <div v-loading="permissionLoading" class="permission-body">
        <p class="permission-tip">勾选权限后保存，将全量覆盖该角色已有权限。</p>
        <el-tree
          ref="treeRef"
          :data="permissionTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'name', children: 'children' }"
        />
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="savingPermissions" @click="savePermissions">
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </PageShell>
</template>

<style scoped>
.role-summary {
  margin-bottom: 16px;
  font-size: 14px;
  color: var(--ys-text-secondary);
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  min-height: 120px;
}

.role-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-sm);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.role-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--ys-shadow-md);
}

.role-card.is-super {
  cursor: default;
}

.role-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.role-heading h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--ys-text-primary);
}

.role-code {
  display: inline-block;
  margin-top: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.role-desc {
  min-height: 40px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ys-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 52px;
  align-content: flex-start;
}

.role-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--ys-border-light);
  flex-wrap: wrap;
}

.action-hint {
  font-size: 12px;
}

.muted {
  font-size: 12px;
  color: var(--ys-text-secondary);
}

.permission-body {
  min-height: 260px;
  max-height: 52vh;
  overflow-y: auto;
  padding-right: 4px;
}

.permission-tip {
  margin-bottom: 14px;
  font-size: 12px;
  color: var(--ys-text-secondary);
}
</style>
