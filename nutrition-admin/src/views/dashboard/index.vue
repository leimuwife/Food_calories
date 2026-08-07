<template>
  <!-- 整体容器 -->
  <div class="dashboard-container">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="nav-left">
        <h1 class="nav-title">食光笔记后台管理系统</h1>
      </div>
      <div class="nav-right">
        <!-- 管理员下拉菜单 -->
        <el-dropdown trigger="hover" @command="handleDropdownCommand">
          <span class="dropdown-trigger">
            {{ nickname }}
            <el-icon class="el-icon--right">
              <ArrowDown />
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="clear-cache">清理缓存</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <div class="main-content">
      <!-- 左侧侧边栏 -->
      <aside class="sidebar">
        <div class="sidebar-menu">
          <!-- 个人中心 -->
          <div class="menu-item">
            <el-icon color="#409EFF">
              <User />
            </el-icon>
            <span class="menu-text">个人中心</span>
          </div>
          <!-- AI 模型配置 -->
          <div class="menu-item" @click="router.push('/ai-config')">
            <el-icon color="#409EFF">
              <Setting />
            </el-icon>
            <span class="menu-text">AI 模型配置</span>
          </div>
          <!-- 知识库更新 -->
          <div class="menu-item" @click="router.push('/dashvector')">
            <el-icon color="#409EFF">
              <Document />
            </el-icon>
            <span class="menu-text">知识库更新</span>
          </div>
        </div>
      </aside>

      <!-- 右侧主内容区 -->
      <section class="content-area">
        <!-- 留白占位，后续页面内容在此渲染 -->
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, User, Setting, Document } from '@element-plus/icons-vue'

const router = useRouter()

// 管理员昵称
const nickname = ref('管理员')

/**
 * 页面加载时获取本地存储的管理员昵称
 */
onMounted(() => {
  const storedNickname = localStorage.getItem('admin_nickname')
  if (storedNickname) {
    nickname.value = storedNickname
  }
})

/**
 * 处理下拉菜单命令
 * @param command 命令标识
 */
function handleDropdownCommand(command: string) {
  switch (command) {
    case 'clear-cache':
      // 清除 localStorage 后台 token 与本地存储，刷新当前页面
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_nickname')
      localStorage.removeItem('admin_fileIds')
      location.reload()
      break
    case 'logout':
      // 清除本地全部管理员存储，路由跳转至 /login
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_nickname')
      localStorage.removeItem('admin_fileIds')
      window.location.href = '/login'
      break
    default:
      break
  }
}
</script>

<style scoped>
/* 整体容器 */
.dashboard-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5faff;
}

/* 顶部导航栏 */
.top-nav {
  width: 100%;
  height: 60px;
  background-color: #409EFF;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-sizing: border-box;
}

/* 导航栏左侧 */
.nav-left {
  flex: 1;
}

.nav-title {
  font-size: 20px;
  font-weight: bold;
  color: #ffffff;
  margin: 0;
}

/* 导航栏右侧 */
.nav-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

/* 下拉菜单触发器 */
.dropdown-trigger {
  display: flex;
  align-items: center;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.dropdown-trigger:hover {
  opacity: 0.8;
}

/* 主体内容区域 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧侧边栏 */
.sidebar {
  width: calc(100% / 9);
  min-width: 120px;
  background-color: #ffffff;
  border-right: 1px solid #e6f2ff;
  display: flex;
  flex-direction: column;
}

/* 侧边栏菜单 */
.sidebar-menu {
  flex: 1;
  padding-top: 20px;
}

/* 菜单单项 */
.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 0 16px 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.menu-item:hover {
  background-color: #ecf5ff;
}

.menu-text {
  font-size: 14px;
  color: #303133;
  margin-left: 8px;
}

/* 右侧主内容区 */
.content-area {
  flex: 1;
  background-color: #f5faff;
  overflow-y: auto;
}

/* Element Plus 下拉菜单样式覆盖 */
:deep(.el-dropdown-menu) {
  background-color: #ffffff;
}

:deep(.el-dropdown-menu__item) {
  color: #303133;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: #ecf5ff;
}

:deep(.el-dropdown-menu__item--divided) {
  border-top: 1px solid #e6f2ff;
}
</style>