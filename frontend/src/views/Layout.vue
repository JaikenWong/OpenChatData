<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">OpenChat4U</div>
      <el-menu :default-active="activeMenu" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>控制台</span>
        </el-menu-item>
        <el-menu-item index="/tenants">
          <el-icon><Setting /></el-icon>
          <span>租户管理</span>
        </el-menu-item>
        <el-menu-item index="/tables">
          <el-icon><Document /></el-icon>
          <span>表结构</span>
        </el-menu-item>
        <el-menu-item index="/dictionary">
          <el-icon><Notebook /></el-icon>
          <span>字典管理</span>
        </el-menu-item>
        <el-menu-item index="/ask">
          <el-icon><ChatDotRound /></el-icon>
          <span>问数</span>
        </el-menu-item>
        <el-menu-item index="/history">
          <el-icon><Clock /></el-icon>
          <span>历史记录</span>
        </el-menu-item>
        <el-menu-item index="/audit">
          <el-icon><Document /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/rbac">
          <el-icon><Lock /></el-icon>
          <span>权限管理</span>
        </el-menu-item>
        <el-menu-item index="/llm">
          <el-icon><Cpu /></el-icon>
          <span>LLM 配置</span>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><Monitor /></el-icon>
          <span>监控中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span>{{ authStore.userInfo?.tenantName }}</span>
          <el-button link type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
  color: #fff;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  background-color: #2b3a4b;
}
.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}
</style>
