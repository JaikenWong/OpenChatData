<template>
  <div class="llm-config">
    <div class="toolbar">
      <el-button type="primary" @click="showDialog = true">新增 LLM 提供商</el-button>
    </div>

    <el-table :data="providers" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="provider" label="类型" width="120" />
      <el-table-column prop="model" label="模型" width="150" />
      <el-table-column prop="baseUrl" label="Base URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="maxTokens" label="Max Tokens" width="100" />
      <el-table-column prop="temperature" label="Temperature" width="100" />
      <el-table-column prop="isDefault" label="默认" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isDefault ? 'success' : 'info'">{{ row.isDefault ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button size="small" @click="handleSetDefault(row)" :disabled="row.isDefault">设为默认</el-button>
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editingProvider ? '编辑 LLM 提供商' : '新增 LLM 提供商'" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如: DeepSeek-Main" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.provider" style="width: 100%" @change="handleProviderChange">
            <el-option v-for="t in providerTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="form.model" placeholder="例如: deepseek-chat" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="100" :max="8000" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
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
import { getLLMProviders, createLLMProvider, updateLLMProvider, deleteLLMProvider, setDefaultLLMProvider, getLLMProviderTypes } from '@/api'

const providers = ref([])
const loading = ref(false)
const showDialog = ref(false)
const editingProvider = ref(null)
const providerTypes = ref([])

const form = reactive({
  name: '',
  provider: 'deepseek',
  model: 'deepseek-chat',
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
  maxTokens: 2000,
  temperature: 0.1,
  isDefault: false
})

const loadProviders = async () => {
  loading.value = true
  try {
    const res = await getLLMProviders()
    providers.value = res.data
  } catch (error) {
    ElMessage.error('加载 LLM 提供商失败')
  } finally {
    loading.value = false
  }
}

const loadProviderTypes = async () => {
  try {
    const res = await getLLMProviderTypes()
    providerTypes.value = res.data
  } catch (error) {
    providerTypes.value = [
      { value: 'deepseek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com' },
      { value: 'openai', label: 'OpenAI', baseUrl: 'https://api.openai.com' },
      { value: 'qwen', label: '通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1' },
      { value: 'zhipu', label: '智谱AI', baseUrl: 'https://open.bigmodel.cn/api/paas/v4' },
      { value: 'custom', label: '自定义', baseUrl: '' }
    ]
  }
}

const handleProviderChange = (provider) => {
  const type = providerTypes.value.find(t => t.value === provider)
  if (type) {
    form.baseUrl = type.baseUrl
  }
}

const handleEdit = (row) => {
  editingProvider.value = row
  Object.assign(form, row)
  showDialog.value = true
}

const handleSetDefault = async (row) => {
  try {
    await setDefaultLLMProvider(row.id)
    ElMessage.success('设置成功')
    loadProviders()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该 LLM 提供商？', '提示', { type: 'warning' })
    await deleteLLMProvider(row.id)
    ElMessage.success('删除成功')
    loadProviders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    if (editingProvider.value) {
      await updateLLMProvider(editingProvider.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createLLMProvider(form)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    editingProvider.value = null
    Object.assign(form, { name: '', provider: 'deepseek', model: 'deepseek-chat', baseUrl: 'https://api.deepseek.com', apiKey: '', maxTokens: 2000, temperature: 0.1, isDefault: false })
    loadProviders()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  loadProviders()
  loadProviderTypes()
})
</script>

<style scoped>
.llm-config {
  padding: 20px;
}
.toolbar {
  margin-bottom: 20px;
}
</style>
