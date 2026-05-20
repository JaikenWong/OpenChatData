<template>
  <div class="dictionary">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="字典管理" name="dictionary">
        <div class="toolbar">
          <el-input v-model="searchTerm" placeholder="搜索术语" style="width: 200px" @keyup.enter="loadDictionaries" clearable />
          <el-select v-model="filterType" placeholder="类型" style="width: 150px" clearable @change="loadDictionaries">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <el-button type="primary" @click="showDialog = true">新增字典</el-button>
        </div>

        <el-table :data="dictionaries" border v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="term" label="术语" width="150" />
          <el-table-column prop="synonyms" label="同义词" min-width="200" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="isActive" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="脱敏规则" name="masking">
        <div class="toolbar">
          <el-button type="primary" @click="showMaskingDialog = true">新增规则</el-button>
        </div>

        <el-table :data="maskingRules" border v-loading="maskingLoading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="tableName" label="表名" width="150" />
          <el-table-column prop="columnName" label="列名" width="150" />
          <el-table-column prop="maskType" label="脱敏类型" width="120" />
          <el-table-column prop="maskPattern" label="模式" width="150" />
          <el-table-column prop="isActive" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'">{{ row.isActive ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDeleteMasking(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showDialog" :title="editingDict ? '编辑字典' : '新增字典'" width="600px">
      <el-form :model="dictForm" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="dictForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="术语">
          <el-input v-model="dictForm.term" placeholder="标准术语" />
        </el-form-item>
        <el-form-item label="同义词">
          <el-input v-model="dictForm.synonyms" placeholder="多个同义词用逗号分隔" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dictForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitDict">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showMaskingDialog" title="新增脱敏规则" width="600px">
      <el-form :model="maskingForm" label-width="100px">
        <el-form-item label="表名">
          <el-input v-model="maskingForm.tableName" />
        </el-form-item>
        <el-form-item label="列名">
          <el-input v-model="maskingForm.columnName" />
        </el-form-item>
        <el-form-item label="脱敏类型">
          <el-select v-model="maskingForm.maskType" placeholder="请选择脱敏类型" style="width: 100%">
            <el-option v-for="t in maskTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模式">
          <el-input v-model="maskingForm.maskPattern" placeholder="可选，如 2-2 表示显示前后2位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMaskingDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitMasking">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictionaries, createDictionary, updateDictionary, deleteDictionary, getDictionaryTypes, getMaskingRules, createMaskingRule, deleteMaskingRule, getMaskTypes } from '@/api'
import { useAuthStore } from '@/store/auth'

const authStore = useAuthStore()
const activeTab = ref('dictionary')

const dictionaries = ref([])
const loading = ref(false)
const searchTerm = ref('')
const filterType = ref('')
const dictTypes = ref([])
const showDialog = ref(false)
const editingDict = ref(null)

const dictForm = reactive({
  type: 'SYNONYM',
  term: '',
  synonyms: '',
  description: ''
})

const maskingRules = ref([])
const maskingLoading = ref(false)
const showMaskingDialog = ref(false)
const maskTypes = ref([])

const maskingForm = reactive({
  tableName: '',
  columnName: '',
  maskType: 'PARTIAL',
  maskPattern: '2-2'
})

const loadDictionaries = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchTerm.value) params.term = searchTerm.value
    if (filterType.value) params.type = filterType.value
    const res = await getDictionaries(authStore.tenantCode, params)
    dictionaries.value = res.data
  } catch (error) {
    ElMessage.error('加载字典失败')
  } finally {
    loading.value = false
  }
}

const loadDictTypes = async () => {
  try {
    const res = await getDictionaryTypes()
    dictTypes.value = res.data
  } catch (error) {
    dictTypes.value = ['SYNONYM', 'BUSINESS_TERM', 'COLUMN_ALIAS', 'TABLE_ALIAS']
  }
}

const handleEdit = (row) => {
  editingDict.value = row
  Object.assign(dictForm, row)
  showDialog.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该字典？', '提示', { type: 'warning' })
    await deleteDictionary(row.id)
    ElMessage.success('删除成功')
    loadDictionaries()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmitDict = async () => {
  try {
    const data = { ...dictForm, tenantCode: authStore.tenantCode }
    if (editingDict.value) {
      await updateDictionary(editingDict.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createDictionary(data)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    editingDict.value = null
    Object.assign(dictForm, { type: 'SYNONYM', term: '', synonyms: '', description: '' })
    loadDictionaries()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const loadMaskingRules = async () => {
  maskingLoading.value = true
  try {
    const res = await getMaskingRules(authStore.tenantCode)
    maskingRules.value = res.data
  } catch (error) {
    ElMessage.error('加载脱敏规则失败')
  } finally {
    maskingLoading.value = false
  }
}

const loadMaskTypes = async () => {
  try {
    const res = await getMaskTypes()
    maskTypes.value = res.data
  } catch (error) {
    maskTypes.value = [
      { value: 'FULL', label: 'Full Mask' },
      { value: 'PARTIAL', label: 'Partial Mask' },
      { value: 'HASH', label: 'Hash' },
      { value: 'REPLACE', label: 'Replace' },
      { value: 'REGEX', label: 'Regex' }
    ]
  }
}

const handleDeleteMasking = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该规则？', '提示', { type: 'warning' })
    await deleteMaskingRule(row.id)
    ElMessage.success('删除成功')
    loadMaskingRules()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmitMasking = async () => {
  try {
    const data = { ...maskingForm, tenantCode: authStore.tenantCode }
    await createMaskingRule(data)
    ElMessage.success('创建成功')
    showMaskingDialog.value = false
    Object.assign(maskingForm, { tableName: '', columnName: '', maskType: 'PARTIAL', maskPattern: '2-2' })
    loadMaskingRules()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  loadDictionaries()
  loadDictTypes()
  loadMaskingRules()
  loadMaskTypes()
})
</script>

<style scoped>
.dictionary {
  padding: 20px;
}
.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>
