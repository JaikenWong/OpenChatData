<template>
  <div class="tables">
    <el-alert title="提示" description="请先在租户管理中配置数据源并连接成功，然后选择租户同步表结构" type="info" show-icon style="margin-bottom: 20px" />

    <el-form :inline="true">
      <el-form-item label="选择租户">
        <el-select v-model="selectedTenant" @change="loadTables">
          <el-option v-for="t in tenants" :key="t.id" :label="t.name" :value="t.code" />
        </el-select>
      </el-form-item>
    </el-form>

    <div v-if="selectedTenant">
      <div class="toolbar">
        <el-button type="primary" @click="handleSync">同步选中表到 Qdrant</el-button>
        <el-button @click="handleSyncAll">同步全部表</el-button>
      </div>
      <el-table :data="tables" border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="tableName" label="表名" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="viewSchema(row)">查看结构</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="schemaDialog" title="表结构">
      <div v-if="currentSchema">
        <p><strong>表名：</strong>{{ currentSchema.tableName }}</p>
        <p v-if="currentSchema.tableComment"><strong>注释：</strong>{{ currentSchema.tableComment }}</p>
        <el-table :data="currentSchema.columns" border>
          <el-table-column prop="columnName" label="列名" />
          <el-table-column prop="dataType" label="类型" />
          <el-table-column prop="columnComment" label="注释" />
          <el-table-column prop="nullable" label="可空" width="80">
            <template #default="{ row }">
              <el-tag :type="row.nullable ? 'success' : 'danger'" size="small">{{ row.nullable ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTenants, getTables, getTableSchema, syncSchema } from '@/api'

const tenants = ref([])
const tables = ref([])
const selectedTenant = ref('')
const selectedTables = ref([])
const schemaDialog = ref(false)
const currentSchema = ref(null)

const loadTenants = async () => {
  try {
    const res = await getTenants()
    tenants.value = res.data
  } catch (error) {
    ElMessage.error('加载租户失败')
  }
}

const loadTables = async () => {
  if (!selectedTenant.value) return
  try {
    const res = await getTables(selectedTenant.value)
    tables.value = res.data.map(name => ({ tableName: name }))
  } catch (error) {
    ElMessage.error('加载表列表失败')
  }
}

const handleSelectionChange = (selection) => {
  selectedTables.value = selection.map(row => row.tableName)
}

const handleSync = async () => {
  if (selectedTables.value.length === 0) {
    ElMessage.warning('请选择要同步的表')
    return
  }
  try {
    await syncSchema(selectedTenant.value, selectedTables.value)
    ElMessage.success('同步成功')
  } catch (error) {
    ElMessage.error('同步失败：' + error.response?.data?.message)
  }
}

const handleSyncAll = async () => {
  if (tables.value.length === 0) return
  const allTables = tables.value.map(t => t.tableName)
  try {
    await syncSchema(selectedTenant.value, allTables)
    ElMessage.success('同步成功')
  } catch (error) {
    ElMessage.error('同步失败：' + error.response?.data?.message)
  }
}

const viewSchema = async (row) => {
  try {
    const res = await getTableSchema(selectedTenant.value, row.tableName)
    currentSchema.value = res.data
    schemaDialog.value = true
  } catch (error) {
    ElMessage.error('加载表结构失败')
  }
}

onMounted(loadTenants)
</script>
