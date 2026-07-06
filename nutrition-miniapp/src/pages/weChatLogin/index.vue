<template>
  <view class="page-container">
    <view class="cat-decoration">
      <svg viewBox="0 0 120 80" class="cat-svg">
        <circle cx="30" cy="35" r="15" fill="#FFB6C1"/>
        <circle cx="30" cy="20" r="8" fill="#FFB6C1"/>
        <circle cx="30" cy="18" r="4" fill="#FF69B4"/>
        <circle cx="90" cy="35" r="15" fill="#FFB6C1"/>
        <circle cx="90" cy="20" r="8" fill="#FFB6C1"/>
        <circle cx="90" cy="18" r="4" fill="#FF69B4"/>
        <ellipse cx="60" cy="50" rx="40" ry="25" fill="#FFB6C1"/>
        <circle cx="45" cy="45" r="4" fill="#333"/>
        <circle cx="75" cy="45" r="4" fill="#333"/>
        <circle cx="46" cy="44" r="1.5" fill="#fff"/>
        <circle cx="76" cy="44" r="1.5" fill="#fff"/>
        <path d="M60 54 Q57 58 60 62 Q63 58 60 54" stroke="#333" stroke-width="2" fill="none"/>
        <path d="M35 55 Q20 50 15 55 Q20 60 35 55" stroke="#FF69B4" stroke-width="3" fill="none"/>
        <path d="M85 55 Q100 50 105 55 Q100 60 85 55" stroke="#FF69B4" stroke-width="3" fill="none"/>
      </svg>
    </view>

    <view class="login-card">
      <view class="logo-area">
        <view class="logo-circle">
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
        <text class="app-slogan">记录美好生活</text>
      </view>

      <view :class="['login-btn', { 'login-btn-loading': isLoading }]" @tap="handleLoginClick">
        <svg v-if="!isLoading" viewBox="0 0 40 40" class="login-btn-icon">
          <circle cx="20" cy="20" r="16" fill="#fff"/>
          <circle cx="16" cy="18" r="3" fill="#333"/>
          <circle cx="24" cy="18" r="3" fill="#333"/>
          <circle cx="17" cy="17" r="1" fill="#fff"/>
          <circle cx="25" cy="17" r="1" fill="#fff"/>
          <path d="M20 26 Q18 29 20 32 Q22 29 20 26" stroke="#333" stroke-width="1.5" fill="none"/>
        </svg>
        <text class="login-btn-text">{{ isLoading ? '登录中...' : '微信一键登录' }}</text>
      </view>
    </view>

    <view class="footer-hint">
      <text class="hint-text">登录即表示同意</text>
      <text class="hint-link">用户协议</text>
      <text class="hint-text">与</text>
      <text class="hint-link">隐私政策</text>
    </view>

    <u-modal 
      :show="showAccountModal" 
      :show-cancel-button="false"
      :close-on-click-overlay="false"
      class="account-modal"
      @close="showAccountModal = false"
    >
      <template #title>
        <view class="modal-title-wrap">
          <svg viewBox="0 0 40 40" class="modal-title-icon">
            <circle cx="20" cy="20" r="16" fill="#FF69B4"/>
            <circle cx="15" cy="18" r="3" fill="#fff"/>
            <circle cx="25" cy="18" r="3" fill="#fff"/>
            <path d="M20 26 Q18 29 20 32 Q22 29 20 26" stroke="#fff" stroke-width="2" fill="none"/>
          </svg>
          <text class="modal-title-text">选择演示账号</text>
        </view>
      </template>
      <view class="account-list">
        <view 
          v-for="account in testAccounts" 
          :key="account.code" 
          class="account-item"
          @tap="selectAccount(account)"
        >
          <view class="account-avatar">
            <svg viewBox="0 0 50 50" class="avatar-svg">
              <circle cx="25" cy="25" r="22" fill="#FFB6C1"/>
              <circle cx="20" cy="23" r="3" fill="#333"/>
              <circle cx="30" cy="23" r="3" fill="#333"/>
              <path d="M25 31 Q23 34 25 37 Q27 34 25 31" stroke="#333" stroke-width="1.5" fill="none"/>
            </svg>
          </view>
          <view class="account-info">
            <text class="account-name">{{ account.nickname }}</text>
            <text class="account-desc">{{ account.description }}</text>
          </view>
          <view class="account-arrow">
            <svg viewBox="0 0 24 24" class="arrow-svg">
              <path d="M9 6l6 6-6 6" stroke="#FF69B4" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </view>
        </view>
      </view>
    </u-modal>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { wxLogin } from '@/api/weChatLogin/index'

const userStore = useUserStore()
const isLoading = ref(false)
const showAccountModal = ref(false)

interface TestAccount {
  nickname: string
  description: string
  code: string
}

const testAccounts: TestAccount[] = [
  { nickname: '张三', description: '普通用户 - 主账号', code: 'test_code_user1' },
  { nickname: '李四', description: '普通用户 - 互动演示', code: 'test_code_user2' },
  { nickname: '小张营养师', description: '官方营养师账号', code: 'test_code_nutritionist' },
]

function handleLoginClick() {
  if (isLoading.value) return
  console.log('Login button clicked')
  showAccountModal.value = true
}

function selectAccount(account: TestAccount) {
  showAccountModal.value = false
  console.log('Selected account:', account)
  performLogin(account.code)
}

async function performLogin(code: string) {
  isLoading.value = true
  console.log('Starting login with code:', code)

  try {
    console.log('Calling wxLogin API...')
    const apiRes = await wxLogin(code)
    console.log('API response:', apiRes)
    
    if (apiRes.data && apiRes.data.token) {
      userStore.setLogin(apiRes.data.token, apiRes.data.user)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/index/index' })
      }, 1500)
    } else {
      throw new Error('登录失败')
    }
  } catch (e) {
    console.error('Login failed:', e)
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  } finally {
    isLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF9FA 0%, #FFF5F7 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 80rpx;
}

.page-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='2' fill='%23FFB6C1' opacity='0.15'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.page-container > * {
  position: relative;
  z-index: 1;
}

.cat-decoration {
  margin-bottom: 40rpx;
}

.cat-svg {
  width: 240rpx;
  height: 160rpx;
}

.login-card {
  width: 600rpx;
  background: #FFFFFF;
  border-radius: 48rpx;
  padding: 60rpx 48rpx;
  box-shadow: 0 16rpx 48rpx rgba(255, 182, 193, 0.15);
}

.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
}

.logo-circle {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 105, 180, 0.3);
}

.logo-icon {
  width: 120rpx;
  height: 120rpx;
}

.app-name {
  font-size: 40rpx;
  font-weight: 600;
  color: #FF69B4;
  margin-bottom: 8rpx;
}

.app-slogan {
  font-size: 26rpx;
  color: rgba(255, 105, 180, 0.6);
}

.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  border-radius: 32rpx;
  box-shadow: 0 12rpx 32rpx rgba(255, 105, 180, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}

.login-btn:active {
  transform: scale(0.98);
  box-shadow: 0 6rpx 16rpx rgba(255, 105, 180, 0.25);
}

.login-btn-loading {
  opacity: 0.7;
}

.login-btn-icon {
  width: 56rpx;
  height: 56rpx;
}

.login-btn-text {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.footer-hint {
  position: fixed;
  bottom: 60rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.hint-text {
  font-size: 24rpx;
  color: rgba(180, 180, 180, 0.8);
}

.hint-link {
  font-size: 24rpx;
  color: rgba(255, 105, 180, 0.7);
}

.account-modal {
  border-radius: 40rpx !important;
}

.account-modal :deep(.u-modal-content) {
  padding: 0 !important;
}

.account-modal :deep(.u-modal-header) {
  padding: 32rpx 32rpx 0 !important;
  border-bottom: none !important;
}

.account-modal :deep(.u-modal-body) {
  padding: 0 !important;
}

.modal-title-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.modal-title-icon {
  width: 56rpx;
  height: 56rpx;
}

.modal-title-text {
  font-size: 32rpx;
  color: #FF69B4;
  font-weight: 600;
}

.account-list {
  padding: 32rpx;
}

.account-item {
  display: flex;
  align-items: center;
  padding: 28rpx;
  background: #FFF9FA;
  border-radius: 28rpx;
  margin-bottom: 20rpx;
  transition: background 0.2s;
}

.account-item:last-child {
  margin-bottom: 0;
}

.account-item:active {
  background: #FFF0F3;
}

.account-avatar {
  width: 80rpx;
  height: 80rpx;
  margin-right: 24rpx;
}

.avatar-svg {
  width: 80rpx;
  height: 80rpx;
}

.account-info {
  flex: 1;
}

.account-name {
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
  display: block;
  margin-bottom: 6rpx;
}

.account-desc {
  font-size: 24rpx;
  color: rgba(255, 105, 180, 0.6);
}

.account-arrow {
  width: 40rpx;
  height: 40rpx;
}

.arrow-svg {
  width: 40rpx;
  height: 40rpx;
}
</style>