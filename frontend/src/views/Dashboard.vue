<template>
  <div class="dashboard">
    <h2>欢迎使用 OpenChat4U 多租问数平台</h2>
    
    <el-row :gutter="20" style="margin-top: 30px">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="租户数" :value="stats.tenantCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="数据库类型" :value="stats.dbTypeCount">
            <template #suffix>种</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="总查询数" :value="stats.totalQueries">
            <template #suffix>次</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="成功率" :value="stats.successRate" :precision="1">
            <template #suffix>%</template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>最近查询</template>
          <el-table :data="recentQueries" size="small" max-height="300">
            <el-table-column prop="question" label="问题" show-overflow-tooltip />
            <el-table-column prop="resultCount" label="结果" width="80" />
            <el-table-column prop="executionTimeMs" label="耗时(ms)" width="100" />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>数据库分布</template>
          <div class="db-distribution">
            <div v-for="db in dbDistribution" :key="db.type" class="db-item">
              <el-tag :type="getDbTypeColor(db.type)" size="large">{{ db.type }}</el-tag>
              <span class="db-count">{{ db.count }} 个租户</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>系统功能</template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="多数据库支持">PostgreSQL, MySQL, Oracle, SQL Server</el-descriptions-item>
            <el-descriptions-item label="向量检索">Qdrant + 讯飞 Embedding + Rerank</el-descriptions-item>
            <el-descriptions-item label="LLM">DeepSeek API</el-descriptions-item>
            <el-descriptions-item label="字典管理">同义词、业务术语映射</el-descriptions-item>
            <el-descriptions-item label="数据脱敏">5种脱敏策略</el-descriptions-item>
            <el-descriptions-item label="审计日志">全量请求审计追踪</el-descriptions-item>
            <el-descriptions-item label="权限控制">RBAC 角色权限管理</el-descriptions-item>
            <el-descriptions-item label="历史记录">查询历史持久化</el-descriptions-item>
            <el-descriptions-item label="流式输出">SSE 实时响应</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTenants, getHistoryStats } from '@/api'

const stats = reactive({
  tenantCount: 0,
  dbTypeCount: 0,
  totalQueries: 0,
  successRate: 0
})

const recentQueries = ref([])
const dbDistribution = ref([])

const getDbTypeColor = (dbType) => {
  const colors = {
    POSTGRESQL: 'blue',
    MYSQL: 'green',
    ORACLE: 'red',
    SQLSERVER: 'orange'
  }
  return colors[dbType] || 'info'
}

const loadStats = async () => {
  try {
    const tenantsRes = await getTenants()
    const tenants = tenantsRes.data
    stats.tenantCount = tenants.length

    const dbTypes = new Set(tenants.map(t => t.dbType))
    stats.dbTypeCount = dbTypes.size

    const distribution = {}
    tenants.forEach(t => {
      distribution[t.dbType] = (distribution[t.dbType] || 0) + 1
    })
    dbDistribution.value = Object.entries(distribution).map(([type, count]) => ({ type, count }))

    try {
      const historyStats = await getHistoryStats('default')
      stats.totalQueries = historyStats.data.totalQueries || 0
      stats.successRate = historyStats.data.totalQueries > 0 
        ? (historyStats.data.successQueries / historyStats.data.totalQueries * 100) 
        : 0
    } catch (e) {
      stats.totalQueries = 0
      stats.successRate = 0
    }
  } catch (error) {
    console.error('Failed to load stats', error)
  }
}

onMounted(loadStats)
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.db-distribution {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}
.db-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.db-count {
  font-size: 14px;
  color: #666;
}
</style>
