<template>
  <div class="audit">
    <div class="toolbar">
      <el-select v-model="filterAction" placeholder="操作类型" style="width: 150px" clearable @change="loadLogs">
        <el-option label="QUERY" value="QUERY" />
        <el-option label="SCHEMA" value="SCHEMA" />
        <el-option label="DICTIONARY" value="DICTIONARY" />
        <el-option label="MASKING" value="MASKING" />
        <el-option label="HISTORY" value="HISTORY" />
        <el-option label="AUTH" value="AUTH" />
        <el-option label="ADMIN" value="ADMIN" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 250px" @change="loadLogs" />
      <el-button type="primary" @click="loadLogs">刷新</el-button>
    </div>

    <el-table :data="logs" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="action" label="操作" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="requestMethod" label="方法" width="80" />
      <el-table-column prop="requestPath" label="路径" min-width="150" show-overflow-tooltip />
      <el-table-column prop="responseStatus" label="状态码" width="80">
        <template #default="{ row }">
          <el-tag :type="row.responseStatus < 400 ? 'success' : 'danger'" size="small">{{ row.responseStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="executionTimeMs" label="耗时(ms)" width="100" />
      <el-table-column prop="userIp" label="IP" width="150" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @size-change="loadLogs"
      @current-change="loadLogs"
      style="margin-top: 20px; justify-content: center"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const logs = ref([])
const loading = ref(false)
const filterAction = ref('')
const dateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filterAction.value) {
      params.action = filterAction.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getAuditLogs(authStore.tenantCode || 'system', params)
    logs.value = res.data.content
    total.value = res.data.totalElements
  } catch (error) {
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.audit {
  padding: 20px;
}
.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>
