<template>
  <div class="monitor">
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card>
          <el-statistic title="数据源总数" :value="healthData.total">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="健康数据源" :value="healthData.healthy" :value-style="{ color: '#67C23A' }">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="异常数据源" :value="healthData.unhealthy" :value-style="{ color: '#F56C6C' }">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>连接池监控</span>
          <el-button type="primary" size="small" @click="loadMonitor">刷新</el-button>
        </div>
      </template>

      <el-table :data="monitorData" border v-loading="loading">
        <el-table-column prop="tenantCode" label="租户" width="120" />
        <el-table-column prop="jdbcUrl" label="JDBC URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="activeConnections" label="活跃连接" width="100">
          <template #default="{ row }">
            <el-tag :type="row.activeConnections > row.maxPoolSize * 0.8 ? 'danger' : 'success'">
              {{ row.activeConnections }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="idleConnections" label="空闲连接" width="100" />
        <el-table-column prop="totalConnections" label="总连接数" width="100" />
        <el-table-column prop="threadsAwaitingConnection" label="等待线程" width="100" />
        <el-table-column prop="maxPoolSize" label="最大池大小" width="100" />
        <el-table-column prop="minIdle" label="最小空闲" width="100" />
        <el-table-column label="使用率" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.round(row.activeConnections / row.maxPoolSize * 100)" 
              :color="getProgressColor(row.activeConnections / row.maxPoolSize)"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDataSourceMonitor, getDataSourceHealth } from '@/api'

const monitorData = ref([])
const healthData = reactive({
  total: 0,
  healthy: 0,
  unhealthy: 0
})
const loading = ref(false)

const getProgressColor = (ratio) => {
  if (ratio > 0.9) return '#F56C6C'
  if (ratio > 0.7) return '#E6A23C'
  return '#67C23A'
}

const loadMonitor = async () => {
  loading.value = true
  try {
    const res = await getDataSourceMonitor()
    monitorData.value = res.data
  } catch (error) {
    ElMessage.error('加载监控数据失败')
  } finally {
    loading.value = false
  }
}

const loadHealth = async () => {
  try {
    const res = await getDataSourceHealth()
    Object.assign(healthData, res.data)
  } catch (error) {
    console.error('加载健康数据失败', error)
  }
}

onMounted(() => {
  loadMonitor()
  loadHealth()
})
</script>

<style scoped>
.monitor {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
