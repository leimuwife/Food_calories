<template>
  <view class="auth-page">
    <view class="brand-block">
      <view class="logo-wrap">
        <svg viewBox="0 0 80 80" class="logo-icon">
          <circle cx="40" cy="40" r="35" fill="#FF69B4"/>
          <circle cx="32" cy="38" r="5" fill="#fff"/>
          <circle cx="48" cy="38" r="5" fill="#fff"/>
          <circle cx="33" cy="37" r="2" fill="#333"/>
          <circle cx="49" cy="37" r="2" fill="#333"/>
          <path d="M40 48 Q37 52 40 56 Q43 52 40 48" stroke="#fff" stroke-width="3" fill="none"/>
        </svg>
      </view>
      <text class="app-name">食光笔记</text>
      <text class="app-slogan">记录每一餐，遇见更好的自己</text>
    </view>

    <view class="auth-card">
      <text class="card-title">欢迎回来</text>
      <text class="card-subtitle">登录后继续管理你的营养计划</text>

      <view class="form-item">
        <text class="field-label">用户名</text>
        <input
          v-model="form.username"
          class="field-input"
          type="text"
          maxlength="32"
          placeholder="请输入用户名"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="form-item">
        <text class="field-label">密码</text>
        <input
          v-model="form.password"
          class="field-input"
          type="password"
          maxlength="32"
          placeholder="请输入密码"
          placeholder-class="input-placeholder"
          @confirm="handleLogin"
        />
      </view>

      <view :class="['primary-btn', { disabled: isLoading }]" @tap="handleLogin">
        <text class="primary-btn-text">{{ isLoading ? '登录中...' : '登录' }}</text>
      </view>

      <view class="switch-row">
        <text class="switch-tip">还没有账号？</text>
        <text class="switch-link" @tap="goRegister">立即注册</text>
      </view>
    </view>

    <text class="agreement">登录即表示同意用户协议与隐私政策</text>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLoading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

onLoad((options) => {
  const username = options?.username
  if (typeof username === 'string') {
    form.username = decodeURIComponent(username)
  }
})

async function handleLogin() {
  if (isLoading.value) return
  const username = form.username.trim()
  const password = form.password

  if (!username || !password) {
    showToast('请输入用户名和密码')
    return
  }
  if (password.length < 6 || password.length > 32) {
    showToast('密码长度为6-32位')
    return
  }

  isLoading.value = true
  try {
    const response = await login(username, password)
    userStore.setLogin(response.data.token, response.data.user)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 700)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '登录失败，请重试')
  } finally {
    isLoading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register/index' })
}

function showToast(title: string) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;
$light-pink: #FFB6C1;

.auth-page {
  min-height: 100vh;
  padding: 90rpx 44rpx 40rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(180deg, #FFF9FA 0%, #FFF2F6 100%);
  position: relative;
  overflow: hidden;
}

.auth-page::before {
  content: '';
  position: fixed;
  top: -160rpx;
  right: -140rpx;
  width: 420rpx;
  height: 420rpx;
  border-radius: 50%;
  background: rgba(255, 182, 193, 0.22);
}

.brand-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 58rpx;
  position: relative;
  z-index: 1;
}

.logo-wrap {
  width: 150rpx;
  height: 150rpx;
  border-radius: 48rpx;
  background: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 18rpx 40rpx rgba(255, 105, 180, 0.18);
  margin-bottom: 24rpx;
}

.logo-icon {
  width: 110rpx;
  height: 110rpx;
}

.app-name {
  font-size: 46rpx;
  font-weight: 700;
  color: #3D2932;
  letter-spacing: 2rpx;
}

.app-slogan {
  margin-top: 10rpx;
  font-size: 25rpx;
  color: #A58A95;
}

.auth-card {
  width: 100%;
  padding: 48rpx 38rpx 42rpx;
  border-radius: 40rpx;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20rpx 60rpx rgba(197, 120, 154, 0.14);
  position: relative;
  z-index: 1;
}

.card-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #3D2932;
}

.card-subtitle {
  display: block;
  margin-top: 10rpx;
  margin-bottom: 42rpx;
  font-size: 25rpx;
  color: #A58A95;
}

.form-item {
  margin-bottom: 30rpx;
}

.field-label {
  display: block;
  margin-bottom: 14rpx;
  font-size: 27rpx;
  font-weight: 600;
  color: #634752;
}

.field-input {
  width: 100%;
  height: 92rpx;
  padding: 0 28rpx;
  box-sizing: border-box;
  border-radius: 24rpx;
  background: #FFF7FA;
  border: 1rpx solid rgba(255, 105, 180, 0.16);
  color: #3D2932;
  font-size: 29rpx;
}

.input-placeholder {
  color: #C9B5BD;
}

.primary-btn {
  height: 94rpx;
  margin-top: 42rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-color 0%, #FF8DC2 100%);
  box-shadow: 0 14rpx 28rpx rgba(255, 105, 180, 0.24);
}

.primary-btn:active {
  transform: scale(0.985);
}

.primary-btn.disabled {
  opacity: 0.65;
}

.primary-btn-text {
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: 700;
}

.switch-row {
  margin-top: 32rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.switch-tip {
  font-size: 26rpx;
  color: #9B7D89;
}

.switch-link {
  margin-left: 8rpx;
  font-size: 27rpx;
  font-weight: 700;
  color: $primary-color;
}

.agreement {
  margin-top: auto;
  padding-top: 46rpx;
  font-size: 23rpx;
  color: #B79DA7;
  text-align: center;
}
</style>
