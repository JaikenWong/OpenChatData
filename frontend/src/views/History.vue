<template>
  <div class="history">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索问题" style="width: 300px" @keyup.enter="loadHistory" clearable />
      <el-button type="primary" @click="loadHistory">搜索</el-button>
      <el-button @click="keyword = ''; loadHistory()">重置</el-button>
    </div>

    <el-table :data="historyList" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sql" label="SQL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="resultCount" label="结果数" width="100" />
      <el-table-column prop="executionTimeMs" label="耗时(ms)" width="100" />
      <el-table-column prop="isSuccess" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isSuccess ? 'success' : 'danger'">
            {{ row.isSuccess ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @size-change="loadHistory"
      @current-change="loadHistory"
      style="margin-top: 20px; justify-content: center"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHistory, deleteHistory } from '@/api'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const historyList = ref([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadHistory = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    const res = await getHistory(authStore.tenantCode, params)
    historyList.value = res.data.content
    total.value = res.data.totalElements
  } catch (error) {
    ElMessage.error('加载历史记录失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
    await deleteHistory(authStore.tenantCode, row.id)
    ElMessage.success('删除成功')
    loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.history {
  padding: 20px;
}
.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>
