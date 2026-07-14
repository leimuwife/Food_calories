<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">系统管理后台</h1>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>

      <el-form :model="form" class="login-form">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入管理员账号"
            size="large"
            @keyup.enter="handleLogin"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入登录密码"
            size="large"
            @keyup.enter="handleLogin"
            prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminLogin } from '../../api/admin'

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    window.location.href = '/'
  }
})

async function handleLogin() {
  if (!form.username.trim()) {
    errorMessage.value = '请输入管理员账号'
    return
  }
  if (!form.password.trim()) {
    errorMessage.value = '请输入登录密码'
    return
  }

  errorMessage.value = ''
  loading.value = true

  try {
    const response = await adminLogin({
      username: form.username.trim(),
      password: form.password,
    })

    const data = response.data
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_nickname', data.nickname || data.username)
    localStorage.setItem('admin_fileIds', data.fileIds || '')

    window.location.href = '/'
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || error.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 30px;
}

.error-message {
  padding: 12px 16px;
  background: #fef0f0;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #dc2626;
  font-size: 14px;
  margin-bottom: 20px;
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: bold;
}
</style>