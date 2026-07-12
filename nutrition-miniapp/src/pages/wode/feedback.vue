<template>
  <view class="page-container">
    <view class="header-area">
      <view class="back-btn" @tap="goBack">
        <svg viewBox="0 0 48 48" class="back-icon">
          <circle cx="24" cy="24" r="22" fill="#FFFFFF"/>
          <path d="M28 18 L20 24 L28 30" stroke="#FF69B4" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="18" cy="24" r="3" fill="#FF69B4"/>
          <circle cx="19" cy="23" r="1" fill="#fff"/>
        </svg>
      </view>
      <text class="header-title">问题反馈历史</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view 
      scroll-y 
      class="content-scroll"
      @refresherrefresh="onRefresh"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
    >
      <view v-if="feedbackList.length === 0" class="empty-state">
        <svg viewBox="0 0 120 120" class="empty-icon">
          <circle cx="60" cy="60" r="50" fill="#FFB6C1" opacity="0.3"/>
          <circle cx="45" cy="55" r="10" fill="#FFB6C1"/>
          <circle cx="75" cy="55" r="10" fill="#FFB6C1"/>
          <circle cx="45" cy="55" r="5" fill="#333"/>
          <circle cx="75" cy="55" r="5" fill="#333"/>
          <circle cx="46" cy="54" r="2" fill="#fff"/>
          <circle cx="76" cy="54" r="2" fill="#fff"/>
          <ellipse cx="60" cy="72" rx="8" ry="5" fill="#FF69B4"/>
          <path d="M30 40 Q45 25 60 40" stroke="#FFB6C1" stroke-width="4" fill="none"/>
          <path d="M60 40 Q75 25 90 40" stroke="#FFB6C1" stroke-width="4" fill="none"/>
          <rect x="35" y="85" width="50" height="15" rx="7" fill="#FFB6C1"/>
          <rect x="40" y="90" width="40" height="5" rx="2" fill="#FF69B4"/>
        </svg>
        <text class="empty-text">暂无反馈记录</text>
        <text class="empty-hint">遇到问题可以在编辑资料页面提交反馈</text>
      </view>

      <view v-else class="feedback-list">
        <view v-for="item in feedbackList" :key="item.id" class="feedback-card">
          <view class="card-header">
            <view class="status-tag" :class="getStatusClass(item.feedbackStatus)">
              <text class="status-text">{{ getStatusText(item.feedbackStatus) }}</text>
            </view>
            <text class="create-time">{{ formatTime(item.createTime) }}</text>
          </view>
          
          <view class="card-content">
            <text class="feedback-text">{{ item.feedbackContent }}</text>
          </view>

          <view v-if="item.adminReply" class="card-reply">
            <view class="reply-header">
              <svg viewBox="0 0 48 48" class="reply-icon">
                <circle cx="24" cy="24" r="20" fill="#98FB98"/>
                <path d="M18 24 L24 30 L34 18" stroke="#228B22" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <text class="reply-label">管理员回复</text>
            </view>
            <text class="reply-text">{{ item.adminReply }}</text>
          </view>
        </view>
      </view>

      <view class="bottom-spacing"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFeedbackList } from '@/api/wode/wode'
import type { UserFeedback } from '@/api/types'
import { FeedbackStatusMap } from '@/api/types'

const feedbackList = ref<UserFeedback[]>([])
const isRefreshing = ref(false)

function getStatusText(status: number): string {
  return FeedbackStatusMap[status] || '未知'
}

function getStatusClass(status: number): string {
  if (status === 0) return 'status-pending'
  if (status === 1) return 'status-processing'
  if (status === 2) return 'status-completed'
  return 'status-pending'
}

function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

async function loadFeedbackList() {
  try {
    const res = await getFeedbackList()
    feedbackList.value = res.data || []
  } catch (e) {
    console.error('加载反馈列表失败:', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

function onRefresh() {
  isRefreshing.value = true
  loadFeedbackList().finally(() => {
    isRefreshing.value = false
  })
}

function goBack() {
  uni.navigateBack({ delta: 1 })
}

onMounted(() => {
  loadFeedbackList()
})
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

.back-btn {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  width: 64rpx;
  height: 64rpx;
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 32rpx;
}

.empty-text {
  font-size: 36rpx;
  font-weight: 500;
  color: #666;
  margin-bottom: 16rpx;
}

.empty-hint {
  font-size: 28rpx;
  color: #999;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.feedback-card {
  background: $card-bg;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.status-tag {
  padding: 8rpx 24rpx;
  border-radius: 20rpx;
}

.status-pending {
  background: rgba(255, 193, 7, 0.15);
}

.status-pending .status-text {
  color: #FFC107;
}

.status-processing {
  background: rgba(33, 150, 243, 0.15);
}

.status-processing .status-text {
  color: #1976D2;
}

.status-completed {
  background: rgba(76, 175, 80, 0.15);
}

.status-completed .status-text {
  color: #388E3C;
}

.status-text {
  font-size: 26rpx;
  font-weight: 500;
}

.create-time {
  font-size: 26rpx;
  color: #999;
}

.card-content {
  margin-bottom: 24rpx;
}

.feedback-text {
  font-size: 32rpx;
  color: #333;
  line-height: 1.6;
}

.card-reply {
  background: rgba(152, 251, 152, 0.1);
  border-radius: 20rpx;
  padding: 20rpx;
  border-left: 6rpx solid #4CAF50;
}

.reply-header {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.reply-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 12rpx;
}

.reply-label {
  font-size: 26rpx;
  font-weight: 500;
  color: #388E3C;
}

.reply-text {
  font-size: 30rpx;
  color: #555;
  line-height: 1.5;
}

.bottom-spacing {
  height: 60rpx;
}
</style>