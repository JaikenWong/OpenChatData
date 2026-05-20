<template>
  <div class="tenants">
    <div class="toolbar">
      <el-button type="primary" @click="showDialog = true">新增租户</el-button>
    </div>
    <el-table :data="tenants" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="code" label="代码" />
      <el-table-column prop="dbType" label="数据库类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getDbTypeColor(row.dbType)">{{ row.dbType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="jdbcUrl" label="JDBC URL" />
      <el-table-column prop="maxConnections" label="最大连接" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button size="small" @click="handleConnect(row)">连接</el-button>
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editingTenant ? '编辑租户' : '新增租户'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="代码">
          <el-input v-model="form.code" :disabled="!!editingTenant" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="数据库类型">
          <el-select v-model="form.dbType" placeholder="请选择数据库类型" style="width: 100%" @change="handleDbTypeChange">
            <el-option v-for="db in dbTypes" :key="db.value" :label="db.label" :value="db.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="JDBC URL">
          <el-input v-model="form.jdbcUrl" :placeholder="jdbcUrlPlaceholder" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="最大连接数">
          <el-input-number v-model="form.maxConnections" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="连接超时(ms)">
          <el-input-number v-model="form.connectionTimeout" :min="1000" :max="60000" :step="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTenants, createTenant, updateTenant, deleteTenant, connectTenant, getDbTypes } from '@/api'

const tenants = ref([])
const showDialog = ref(false)
const editingTenant = ref(null)
const dbTypes = ref([])

const form = reactive({
  name: '',
  code: '',
  description: '',
  dbType: 'POSTGRESQL',
  jdbcUrl: '',
  username: '',
  password: '',
  maxConnections: 5,
  connectionTimeout: 10000
})

const jdbcUrlPlaceholder = ref('jdbc:postgresql://host:port/db')

const getDbTypeColor = (dbType) => {
  const colors = {
    POSTGRESQL: 'blue',
    MYSQL: 'green',
    ORACLE: 'red',
    SQLSERVER: 'orange'
  }
  return colors[dbType] || 'info'
}

const handleDbTypeChange = (dbType) => {
  const placeholders = {
    POSTGRESQL: 'jdbc:postgresql://host:port/db',
    MYSQL: 'jdbc:mysql://host:port/db',
    ORACLE: 'jdbc:oracle:thin:@host:port:db',
    SQLSERVER: 'jdbc:sqlserver://host:port;databaseName=db'
  }
  jdbcUrlPlaceholder.value = placeholders[dbType] || 'jdbc:...'
}

const loadTenants = async () => {
  try {
    const res = await getTenants()
    tenants.value = res.data
  } catch (error) {
    ElMessage.error('加载租户列表失败')
  }
}

const loadDbTypes = async () => {
  try {
    const res = await getDbTypes()
    dbTypes.value = res.data
  } catch (error) {
    dbTypes.value = [
      { value: 'POSTGRESQL', label: 'PostgreSQL' },
      { value: 'MYSQL', label: 'MySQL' },
      { value: 'ORACLE', label: 'Oracle' },
      { value: 'SQLSERVER', label: 'SQL Server' }
    ]
  }
}

const handleConnect = async (row) => {
  try {
    await connectTenant(row.id)
    ElMessage.success('连接成功')
  } catch (error) {
    ElMessage.error('连接失败：' + error.response?.data?.message)
  }
}

const handleEdit = (row) => {
  editingTenant.value = row
  Object.assign(form, row)
  handleDbTypeChange(row.dbType)
  showDialog.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该租户？', '提示', { type: 'warning' })
    await deleteTenant(row.id)
    ElMessage.success('删除成功')
    loadTenants()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    if (editingTenant.value) {
      await updateTenant(editingTenant.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createTenant(form)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    editingTenant.value = null
    Object.assign(form, { name: '', code: '', description: '', dbType: 'POSTGRESQL', jdbcUrl: '', username: '', password: '', maxConnections: 5, connectionTimeout: 10000 })
    loadTenants()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  loadTenants()
  loadDbTypes()
})
</script>
