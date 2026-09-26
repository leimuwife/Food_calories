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
      <text class="header-title">作者简介</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view scroll-y class="content-scroll">
      <view class="author-hero">
        <view class="author-avatar">
          <svg viewBox="0 0 100 100" class="avatar-icon">
            <circle cx="50" cy="50" r="45" fill="#FFB6C1"/>
            <circle cx="38" cy="42" r="6" fill="#333"/>
            <circle cx="62" cy="42" r="6" fill="#333"/>
            <circle cx="39" cy="41" r="2" fill="#fff"/>
            <circle cx="63" cy="41" r="2" fill="#fff"/>
            <ellipse cx="50" cy="56" rx="5" ry="3" fill="#FF69B4"/>
            <path d="M42 64 Q50 70 58 64" stroke="#333" stroke-width="2" fill="none"/>
            <circle cx="28" cy="35" r="8" fill="#FFB6C1"/>
            <circle cx="72" cy="35" r="8" fill="#FFB6C1"/>
            <path d="M20 35 Q28 25 36 35" stroke="#FFB6C1" stroke-width="3" fill="none"/>
            <path d="M64 35 Q72 25 80 35" stroke="#FFB6C1" stroke-width="3" fill="none"/>
          </svg>
        </view>
        <text class="author-title">吴聪聪</text>
        <text class="author-intro">软件工程专业，双非本科生，热爱后端开发与AI应用开发，持续记录技术学习与项目实践。</text>
      </view>

      <view class="contact-list">
        <view class="contact-card" @tap="openGithub">
          <view class="contact-icon-wrap">
            <svg viewBox="0 0 48 48" class="contact-icon">
              <circle cx="24" cy="24" r="20" fill="#FFF0F3"/>
              <path d="M17 30 C15 30 15 27 17 26 C15 25 15 22 17 21 C18 18 20 18 21 19 L22 18 L26 18 L27 19 C28 18 30 18 31 21 C33 22 33 25 31 26 C33 27 33 30 31 30" stroke="#FF69B4" stroke-width="2" fill="none" stroke-linejoin="round"/>
              <path d="M20 28 L28 28 M20 25 L28 25" stroke="#FF69B4" stroke-width="2" fill="none" stroke-linecap="round"/>
              <path d="M24 18 L24 30" stroke="#FF69B4" stroke-width="2" fill="none" stroke-linecap="round"/>
            </svg>
          </view>
          <view class="contact-info">
            <text class="contact-label">GitHub</text>
            <text class="contact-value">github.com/leimuwife</text>
          </view>
        </view>

        <view class="contact-card" @tap="sendEmail">
          <view class="contact-icon-wrap">
            <svg viewBox="0 0 48 48" class="contact-icon">
              <circle cx="24" cy="24" r="20" fill="#FFF0F3"/>
              <rect x="13" y="17" width="22" height="15" rx="3" fill="#FFB6C1"/>
              <path d="M15 20 L24 26 L33 20" stroke="#FF69B4" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </view>
          <view class="contact-info">
            <text class="contact-label">邮箱</text>
            <text class="contact-value">wccleimu@outlook.com</text>
          </view>
        </view>
      </view>

      <view class="footer-note">
        <text class="footer-note-text">持续记录技术学习与项目实践</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
function goBack() {
  uni.navigateBack({ delta: 1 })
}

function openGithub() {
  // #ifdef H5
  window.open('https://github.com/leimuwife', '_blank')
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: 'https://github.com/leimuwife',
    success: () => {
      uni.showToast({ title: '链接已复制', icon: 'none' })
    }
  })
  // #endif
}

function sendEmail() {
  const subject = encodeURIComponent('来自食光笔记的邮件')
  const body = encodeURIComponent('你好，吴聪聪：\n\n')
  // #ifdef H5
  window.location.href = `mailto:wccleimu@outlook.com?subject=${subject}&body=${body}`
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: 'wccleimu@outlook.com',
    success: () => {
      uni.showToast({ title: '邮箱已复制', icon: 'none' })
    }
  })
  // #endif
}
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
  justify-content: space-between;
  padding: 60rpx 32rpx 32rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
}

.back-btn {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.back-btn:active {
  transform: scale(0.95);
}

.back-icon {
  width: 64rpx;
  height: 64rpx;
}

.header-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.placeholder {
  width: 80rpx;
}

.content-scroll {
  flex: 1;
  padding: 32rpx 28rpx;
  box-sizing: border-box;
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.author-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 24rpx 56rpx;
}

.author-avatar {
  width: 168rpx;
  height: 168rpx;
  border-radius: 50%;
  background: $card-bg;
  border: 6rpx solid rgba(255, 182, 193, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 36rpx;
  box-shadow: 0 12rpx 32rpx rgba(255, 105, 180, 0.14);
}

.avatar-icon {
  width: 140rpx;
  height: 140rpx;
}

.author-title {
  font-size: 52rpx;
  font-weight: 700;
  color: $primary-color;
  letter-spacing: 2rpx;
}

.author-intro {
  margin-top: 24rpx;
  max-width: 640rpx;
  text-align: center;
  font-size: 28rpx;
  line-height: 1.9;
  color: #666;
}

.contact-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
  align-items: stretch;
}

.contact-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: $card-bg;
  border-radius: 28rpx;
  padding: 32rpx 20rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.28);
  box-shadow: 0 8rpx 24rpx rgba(255, 105, 180, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  min-height: 220rpx;
}

.contact-card:active {
  transform: translateY(-4rpx);
  box-shadow: 0 14rpx 32rpx rgba(255, 105, 180, 0.16);
}

.contact-card:hover {
  transform: translateY(-4rpx);
  box-shadow: 0 14rpx 32rpx rgba(255, 105, 180, 0.16);
}

.contact-icon-wrap {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #FFF0F3 0%, #FFE4E9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.22);
}

.contact-icon {
  width: 64rpx;
  height: 64rpx;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  align-items: center;
  min-width: 0;
  width: 100%;
}

.contact-label {
  font-size: 28rpx;
  font-weight: 600;
  color: $primary-color;
}

.contact-value {
  font-size: 24rpx;
  color: #666;
  word-break: break-all;
  line-height: 1.5;
}

.footer-note {
  text-align: center;
  padding: 56rpx 0 32rpx;
}

.footer-note-text {
  font-size: 24rpx;
  color: #B8B8B8;
}
</style>
