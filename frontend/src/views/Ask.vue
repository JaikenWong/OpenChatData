<template>
  <div class="ask">
    <div class="chat-container">
      <div class="messages" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
          <div class="avatar">
            <el-icon v-if="msg.role === 'user'"><User /></el-icon>
            <el-icon v-else><Cpu /></el-icon>
          </div>
          <div class="content">
            <div class="text">{{ msg.text }}</div>
            <div v-if="msg.sql" class="sql-block">
              <div class="sql-label">SQL:</div>
              <pre>{{ msg.sql }}</pre>
            </div>
            <div v-if="msg.chart" class="chart-container">
              <div class="chart-header">
                <span>图表展示</span>
                <el-select v-model="msg.chartType" size="small" style="width: 120px" @change="changeChartType(msg)">
                  <el-option label="柱状图" value="bar" />
                  <el-option label="折线图" value="line" />
                  <el-option label="饼图" value="pie" />
                  <el-option label="表格" value="table" />
                </el-select>
              </div>
              <div :ref="el => setChartRef(el, index)" class="chart" style="height: 300px;"></div>
            </div>
            <div v-if="msg.data && msg.data.length" class="data-block">
              <el-table :data="msg.data" border size="small" max-height="300">
                <el-table-column v-for="key in Object.keys(msg.data[0])" :key="key" :prop="key" :label="key" />
              </el-table>
            </div>
          </div>
        </div>
        <div v-if="loading" class="message ai">
          <div class="avatar"><el-icon><Cpu /></el-icon></div>
          <div class="content">思考中...</div>
        </div>
      </div>
      <div class="input-area">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题，例如：上个月销售额是多少？"
          @keydown.enter.exact="handleSend"
        />
        <el-button type="primary" @click="handleSend" :loading="loading">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ask } from '@/api'
import * as echarts from 'echarts'

const messages = ref([])
const question = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const chartRefs = ref({})
const chartInstances = ref({})

const setChartRef = (el, index) => {
  if (el) {
    chartRefs.value[index] = el
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const renderChart = (msg, index) => {
  if (!chartRefs.value[index]) return
  
  if (chartInstances.value[index]) {
    chartInstances.value[index].dispose()
  }
  
  const chart = echarts.init(chartRefs.value[index])
  chartInstances.value[index] = chart
  
  const option = generateChartOption(msg.chartType, msg.chart, msg.data)
  chart.setOption(option)
}

const generateChartOption = (type, chart, data) => {
  if (!chart || !data || data.length === 0) {
    return {}
  }
  
  switch (type) {
    case 'bar':
      return {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: chart.categories || [] },
        yAxis: { type: 'value' },
        series: (chart.series || []).map(s => ({
          name: s.name,
          type: 'bar',
          data: s.data || []
        }))
      }
    case 'line':
      return {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: chart.categories || [] },
        yAxis: { type: 'value' },
        series: (chart.series || []).map(s => ({
          name: s.name,
          type: 'line',
          data: s.data || [],
          smooth: true
        }))
      }
    case 'pie':
      return {
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: '50%',
          data: (chart.categories || []).map((name, i) => ({
            name,
            value: (chart.series?.[0]?.data || [])[i] || 0
          }))
        }]
      }
    default:
      return {}
  }
}

const changeChartType = (msg) => {
  const index = messages.value.indexOf(msg)
  renderChart(msg, index)
}

const handleSend = async () => {
  if (!question.value.trim() || loading.value) return

  const userQuestion = question.value.trim()
  messages.value.push({ role: 'user', text: userQuestion })
  question.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const res = await ask({ question: userQuestion })
    const data = res.data

    if (data.error) {
      messages.value.push({ role: 'ai', text: '错误：' + data.error })
    } else {
      const msg = {
        role: 'ai',
        text: data.answer,
        sql: data.sql,
        data: data.data,
        chart: data.chart,
        chartType: data.chart?.chartType || 'bar'
      }
      messages.value.push(msg)
      
      await nextTick()
      if (data.chart) {
        const index = messages.value.length - 1
        renderChart(msg, index)
      }
    }
  } catch (error) {
    messages.value.push({ role: 'ai', text: '请求失败：' + (error.response?.data?.message || error.message) })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

onMounted(() => {
  window.addEventListener('resize', () => {
    Object.values(chartInstances.value).forEach(chart => chart?.resize())
  })
})
</script>

<style scoped>
.ask {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.message {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}
.message.user {
  flex-direction: row-reverse;
}
.message .avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.message.ai .avatar {
  background: #409EFF;
  color: #fff;
}
.message.user .avatar {
  background: #67C23A;
  color: #fff;
}
.content {
  max-width: 70%;
  background: #f5f5f5;
  padding: 15px;
  border-radius: 10px;
}
.message.user .content {
  background: #409EFF;
  color: #fff;
}
.sql-block {
  margin-top: 10px;
  background: rgba(0,0,0,0.1);
  padding: 10px;
  border-radius: 5px;
}
.sql-label {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 5px;
}
.sql-block pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: monospace;
  font-size: 12px;
}
.chart-container {
  margin-top: 10px;
  background: #fff;
  border-radius: 5px;
  padding: 10px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: bold;
}
.chart {
  width: 100%;
}
.data-block {
  margin-top: 10px;
}
.input-area {
  padding: 20px;
  background: #fff;
  border-top: 1px solid #e6e6e6;
  display: flex;
  gap: 10px;
}
</style>
