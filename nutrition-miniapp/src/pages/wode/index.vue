<template>
  <view class="page-container">
    <view class="header-area">
      <text class="header-title">个人中心</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view 
      scroll-y 
      class="content-scroll"
      @refreshrefresh="onRefresh"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
    >
      <view class="profile-card">
        <view class="avatar-section">
          <image :src="avatarUrl" class="user-avatar" mode="aspectFill"/>
        </view>
        <view class="user-info">
          <text class="user-nickname">{{ userInfo.nickname }}</text>
        </view>
      </view>

      <view class="menu-card">
        <view class="menu-item" @tap="goToEdit">
          <view class="menu-icon-wrap">
            <svg viewBox="0 0 48 48" class="menu-icon">
              <rect x="8" y="12" width="32" height="28" rx="4" fill="#FFB6C1"/>
              <path d="M16 20 L32 20" stroke="#FF69B4" stroke-width="2" fill="none"/>
              <path d="M16 26 L28 26" stroke="#FF69B4" stroke-width="2" fill="none"/>
              <path d="M20 32 L24 32" stroke="#FF69B4" stroke-width="2" fill="none"/>
            </svg>
          </view>
          <text class="menu-text">绑定邮箱</text>
          <text class="menu-value">{{ userInfo.email || '未设置' }}</text>
          <svg viewBox="0 0 48 48" class="menu-arrow">
            <path d="M18 20 L24 26 L18 32" stroke="#FFB6C1" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </view>

        <view class="menu-item" @tap="goToFeedback">
          <view class="menu-icon-wrap">
            <svg viewBox="0 0 48 48" class="menu-icon">
              <circle cx="24" cy="24" r="20" fill="#FFB6C1"/>
              <path d="M20 18 L28 18 L28 28 L20 28 Z" stroke="#FF69B4" stroke-width="2" fill="none"/>
              <path d="M20 32 L28 32" stroke="#FF69B4" stroke-width="2" fill="none"/>
            </svg>
          </view>
          <text class="menu-text">问题反馈</text>
          <svg viewBox="0 0 48 48" class="menu-arrow">
            <path d="M18 20 L24 26 L18 32" stroke="#FFB6C1" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </view>

        <view class="menu-item">
          <view class="menu-icon-wrap">
            <svg viewBox="0 0 48 48" class="menu-icon">
              <circle cx="24" cy="24" r="20" fill="#FFB6C1"/>
              <text x="24" y="30" text-anchor="middle" fill="#FF69B4" font-size="12" font-weight="bold">V1</text>
            </svg>
          </view>
          <text class="menu-text">版本号</text>
          <text class="menu-value">V1.0.0</text>
        </view>

        <view class="menu-item" @tap="showAbout">
          <view class="menu-icon-wrap">
            <svg viewBox="0 0 48 48" class="menu-icon">
              <circle cx="24" cy="24" r="20" fill="#FFB6C1"/>
              <circle cx="24" cy="20" r="3" fill="#FF69B4"/>
              <path d="M24 24 L24 32" stroke="#FF69B4" stroke-width="2" fill="none"/>
              <path d="M18 28 L30 28" stroke="#FF69B4" stroke-width="2" fill="none"/>
            </svg>
          </view>
          <text class="menu-text">关于本站</text>
          <svg viewBox="0 0 48 48" class="menu-arrow">
            <path d="M18 20 L24 26 L18 32" stroke="#FFB6C1" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </view>

        <view class="menu-item" @tap="showNotice">
          <view class="menu-icon-wrap">
            <svg viewBox="0 0 48 48" class="menu-icon">
              <rect x="8" y="10" width="32" height="30" rx="4" fill="#FFB6C1"/>
              <path d="M16 18 L32 18" stroke="#FF69B4" stroke-width="2" fill="none"/>
              <path d="M16 24 L28 24" stroke="#FF69B4" stroke-width="2" fill="none"/>
              <path d="M16 30 L24 30" stroke="#FF69B4" stroke-width="2" fill="none"/>
            </svg>
          </view>
          <text class="menu-text">责任说明公告</text>
          <svg viewBox="0 0 48 48" class="menu-arrow">
            <path d="M18 20 L24 26 L18 32" stroke="#FFB6C1" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </view>
      </view>

      <view class="bottom-spacing"></view>
    </scroll-view>

    <view class="footer-area">
      <view class="edit-btn" @tap="goToEdit">
        <svg viewBox="0 0 48 48" class="edit-icon">
          <rect x="8" y="8" width="32" height="32" rx="4" fill="#FF69B4"/>
          <path d="M28 16 L32 16 L32 20" stroke="#fff" stroke-width="2" fill="none"/>
          <path d="M16 24 L32 24" stroke="#fff" stroke-width="2" fill="none"/>
          <path d="M16 32 L24 32" stroke="#fff" stroke-width="2" fill="none"/>
        </svg>
        <text class="edit-text">编辑资料</text>
      </view>
    </view>

    <view v-if="showPopup" class="popup-overlay" @tap="closePopup">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">{{ popupTitle }}</text>
        </view>
        <scroll-view scroll-y class="popup-body">
          <text class="popup-text">{{ popupContent }}</text>
        </scroll-view>
        <view class="popup-footer">
          <view class="close-btn" @tap="closePopup">
            <text class="close-text">关闭</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getAttachmentUrl } from '@/api'

const userStore = useUserStore()
const isRefreshing = ref(false)
const showPopup = ref(false)
const popupTitle = ref('')
const popupContent = ref('')

const userInfo = computed(() => {
  const user = userStore.userInfo
  if (!user) {
    return {
      nickname: generateDefaultNickname(),
      email: '',
      fileIds: null
    }
  }
  return {
    nickname: user.nickname || generateDefaultNickname(),
    email: user.email || '',
    fileIds: user.fileIds || null
  }
})

const avatarUrl = computed(() => {
  if (!userInfo.value.fileIds) {
    return getDefaultAvatar()
  }
  let firstId: string | null = null
  try {
    const ids = JSON.parse(userInfo.value.fileIds)
    if (Array.isArray(ids) && ids.length > 0) {
      firstId = String(ids[0])
    }
  } catch {
    const parts = userInfo.value.fileIds.split(',')
    if (parts.length > 0) {
      firstId = parts[0].trim()
    }
  }
  return firstId ? getAttachmentUrl(Number(firstId)) : getDefaultAvatar()
})

function generateDefaultNickname(): string {
  const randomNum = Math.floor(100000 + Math.random() * 900000)
  return `用户${randomNum}`
}

function getDefaultAvatar(): string {
  return 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"%3E%3Ccircle cx="50" cy="50" r="45" fill="%23FFB6C1"/%3E%3Ccircle cx="38" cy="42" r="6" fill="%23333"/%3E%3Ccircle cx="62" cy="42" r="6" fill="%23333"/%3E%3Ccircle cx="39" cy="41" r="2" fill="%23fff"/%3E%3Ccircle cx="63" cy="41" r="2" fill="%23fff"/%3E%3Cellipse cx="50" cy="56" rx="5" ry="3" fill="%23FF69B4"/%3E%3Cpath d="M42 64 Q50 70 58 64" stroke="%23333" stroke-width="2" fill="none"/%3E%3Ccircle cx="28" cy="35" r="8" fill="%23FFB6C1"/%3E%3Ccircle cx="72" cy="35" r="8" fill="%23FFB6C1"/%3E%3Cpath d="M20 35 Q28 25 36 35" stroke="%23FFB6C1" stroke-width="3" fill="none"/%3E%3Cpath d="M64 35 Q72 25 80 35" stroke="%23FFB6C1" stroke-width="3" fill="none"/%3E%3C/svg%3E'
}

function goToEdit() {
  uni.navigateTo({ url: '/pages/wode/edit' })
}

function goToFeedback() {
  uni.navigateTo({ url: '/pages/wode/edit?tab=feedback' })
}

function showAbout() {
  popupTitle.value = '关于本站'
  popupContent.value = '食光笔记是一款专注于健康饮食管理的应用，帮助您记录每日饮食、控制热量摄入、分享减肥日常。我们致力于为用户提供一个温暖、治愈的健康管理平台，让每一次饮食记录都成为美好回忆。'
  showPopup.value = true
}

function showNotice() {
  popupTitle.value = '责任说明公告'
  popupContent.value = '本应用提供的热量计算仅供参考，实际热量可能因食材品种、烹饪方式等因素有所差异。用户在使用本应用进行饮食管理时，应结合自身情况合理安排饮食。本应用不对用户因使用本应用而产生的任何健康问题承担责任。如有健康疑问，请咨询专业医师或营养师。'
  showPopup.value = true
}

function closePopup() {
  showPopup.value = false
}

function onRefresh() {
  isRefreshing.value = true
  setTimeout(() => {
    isRefreshing.value = false
    uni.showToast({ title: '刷新成功', icon: 'success' })
  }, 1000)
}

onMounted(() => {})
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;
$light-pink: #FFB6C1;
$bg-color: #FFF9FA;
$card-bg: #FFFFFF;

.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, $bg-color 0%, #FFF5F7 100%);
  display: flex;
  flex-direction: column;
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

.header-area {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx 32rpx 32rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
}

.header-title {
  font-size: 44rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.placeholder {
  width: 80rpx;
}

.content-scroll {
  flex: 1;
  padding: 24rpx;
}

.profile-card {
  background: linear-gradient(135deg, rgba(255, 105, 180, 0.1) 0%, rgba(255, 182, 193, 0.1) 100%);
  border-radius: 40rpx;
  padding: 48rpx;
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.avatar-section {
  margin-bottom: 24rpx;
}

.user-avatar {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  border: 6rpx solid $primary-color;
  background: #F5F5F5;
}

.user-info {
  text-align: center;
}

.user-nickname {
  font-size: 40rpx;
  font-weight: 600;
  color: #333;
}

.menu-card {
  background: $card-bg;
  border-radius: 32rpx;
  padding: 16rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  transition: all 0.2s;
}

.menu-item:active {
  background: rgba(255, 182, 193, 0.1);
}

.menu-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.menu-icon {
  width: 48rpx;
  height: 48rpx;
}

.menu-text {
  flex: 1;
  font-size: 32rpx;
  color: #333;
}

.menu-value {
  font-size: 28rpx;
  color: #999;
  margin-right: 16rpx;
}

.menu-arrow {
  width: 32rpx;
  height: 32rpx;
}

.bottom-spacing {
  height: 200rpx;
}

.footer-area {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $card-bg;
  box-shadow: 0 -8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 32rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 40rpx;
  transition: all 0.2s;
}

.edit-btn:active {
  transform: scale(0.98);
}

.edit-icon {
  width: 48rpx;
  height: 48rpx;
}

.edit-text {
  font-size: 34rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.popup-content {
  width: 100%;
  background: $card-bg;
  border-radius: 48rpx 48rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.popup-header {
  padding: 32rpx;
  text-align: center;
  border-bottom: 1rpx solid rgba(255, 182, 193, 0.3);
}

.popup-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $primary-color;
}

.popup-body {
  flex: 1;
  padding: 32rpx;
  max-height: 480rpx;
}

.popup-text {
  font-size: 30rpx;
  color: #666;
  line-height: 1.8;
}

.popup-footer {
  padding: 24rpx 32rpx;
  border-top: 1rpx solid rgba(255, 182, 193, 0.3);
}

.close-btn {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 40rpx;
  padding: 28rpx;
  text-align: center;
  transition: all 0.2s;
}

.close-btn:active {
  transform: scale(0.98);
}

.close-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
}
</style>