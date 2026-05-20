<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">OpenChat4U 登录</h2>
      <el-form :model="form" label-width="100px">
        <el-form-item label="租户代码">
          <el-input v-model="form.tenantCode" placeholder="请输入租户代码" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  tenantCode: '',
  username: '',
  password: ''
})

const handleLogin = async () => {
  try {
    const res = await login(form)
    authStore.setToken(res.data.token, {
      username: res.data.username,
      tenantId: res.data.tenantId,
      tenantCode: res.data.tenantCode,
      tenantName: res.data.tenantName
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
}
.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
</style>
