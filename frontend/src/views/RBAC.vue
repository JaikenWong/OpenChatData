<template>
  <div class="rbac">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="角色管理" name="roles">
        <div class="toolbar">
          <el-button type="primary" @click="showRoleDialog = true">新增角色</el-button>
        </div>

        <el-table :data="roles" border v-loading="rolesLoading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="角色名" width="150" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column prop="tenantCode" label="租户" width="120" />
          <el-table-column prop="isSystem" label="系统角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isSystem ? 'warning' : 'info'">{{ row.isSystem ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="handleEditPermissions(row)">权限</el-button>
              <el-button size="small" @click="handleEditRole(row)">编辑</el-button>
              <el-button size="small" type="danger" :disabled="row.isSystem" @click="handleDeleteRole(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="权限列表" name="permissions">
        <el-table :data="permissions" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="code" label="权限代码" width="150" />
          <el-table-column prop="name" label="权限名称" width="150" />
          <el-table-column prop="resourceType" label="资源类型" width="120" />
          <el-table-column prop="action" label="操作" width="100" />
          <el-table-column prop="description" label="描述" min-width="200" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showRoleDialog" :title="editingRole ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="角色名">
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="租户">
          <el-input v-model="roleForm.tenantCode" placeholder="留空为系统角色" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRole">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPermissionDialog" title="编辑权限" width="600px">
      <el-transfer v-model="selectedPermissions" :data="permissionOptions" :titles="['可选权限', '已选权限']" />
      <template #footer>
        <el-button @click="showPermissionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, createRole, updateRole, deleteRole, getPermissions, getRolePermissions, assignPermissions } from '@/api'

const activeTab = ref('roles')
const roles = ref([])
const rolesLoading = ref(false)
const permissions = ref([])
const showRoleDialog = ref(false)
const editingRole = ref(null)
const showPermissionDialog = ref(false)
const currentRole = ref(null)
const selectedPermissions = ref([])

const roleForm = reactive({
  name: '',
  description: '',
  tenantCode: ''
})

const permissionOptions = computed(() => {
  return permissions.value.map(p => ({
    key: p.id,
    label: `${p.name} (${p.code})`
  }))
})

const loadRoles = async () => {
  rolesLoading.value = true
  try {
    const res = await getRoles()
    roles.value = res.data
  } catch (error) {
    ElMessage.error('加载角色失败')
  } finally {
    rolesLoading.value = false
  }
}

const loadPermissions = async () => {
  try {
    const res = await getPermissions()
    permissions.value = res.data
  } catch (error) {
    console.error('加载权限失败', error)
  }
}

const handleEditRole = (row) => {
  editingRole.value = row
  Object.assign(roleForm, row)
  showRoleDialog.value = true
}

const handleDeleteRole = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该角色？', '提示', { type: 'warning' })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmitRole = async () => {
  try {
    if (editingRole.value) {
      await updateRole(editingRole.value.id, roleForm)
      ElMessage.success('更新成功')
    } else {
      await createRole(roleForm)
      ElMessage.success('创建成功')
    }
    showRoleDialog.value = false
    editingRole.value = null
    Object.assign(roleForm, { name: '', description: '', tenantCode: '' })
    loadRoles()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const handleEditPermissions = async (row) => {
  currentRole.value = row
  try {
    const res = await getRolePermissions(row.id)
    selectedPermissions.value = res.data.map(p => p.id)
  } catch (error) {
    selectedPermissions.value = []
  }
  showPermissionDialog.value = true
}

const handleSavePermissions = async () => {
  try {
    await assignPermissions(currentRole.value.id, selectedPermissions.value)
    ElMessage.success('保存成功')
    showPermissionDialog.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.rbac {
  padding: 20px;
}
.toolbar {
  margin-bottom: 20px;
}
</style>
