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
      <text class="header-title">编辑个人资料</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view scroll-y class="content-scroll">
      <view class="edit-card">
        <view class="avatar-section">
          <view class="avatar-label">
            <svg viewBox="0 0 48 48" class="label-icon">
              <circle cx="24" cy="24" r="20" fill="#FFB6C1"/>
              <circle cx="24" cy="24" r="8" fill="#FF69B4"/>
            </svg>
            <text class="label-text">头像</text>
          </view>
          <view class="avatar-preview" @tap="chooseAvatar">
            <image :src="avatarUrl" class="avatar-img" mode="aspectFill"/>
            <view class="avatar-edit">
              <svg viewBox="0 0 48 48" class="edit-icon">
                <circle cx="24" cy="24" r="20" fill="#FF69B4"/>
                <path d="M20 24 L24 20 L28 24 L24 28 Z" stroke="#fff" stroke-width="2" fill="none"/>
              </svg>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-item">
            <view class="form-label">
              <svg viewBox="0 0 48 48" class="label-icon">
                <circle cx="24" cy="24" r="20" fill="#FFB6C1"/>
                <path d="M20 20 L28 20 L28 28 L20 28 Z" stroke="#FF69B4" stroke-width="2" fill="none"/>
              </svg>
              <text class="label-text">昵称</text>
            </view>
            <input 
              v-model="nickname" 
              class="form-input" 
              placeholder="请输入昵称" 
              :maxlength="20"
            />
          </view>

          <view class="form-item">
            <view class="form-label">
              <svg viewBox="0 0 48 48" class="label-icon">
                <rect x="8" y="12" width="32" height="28" rx="4" fill="#FFB6C1"/>
                <path d="M16 20 L32 20" stroke="#FF69B4" stroke-width="2" fill="none"/>
                <path d="M16 26 L28 26" stroke="#FF69B4" stroke-width="2" fill="none"/>
              </svg>
              <text class="label-text">邮箱</text>
            </view>
            <input 
              v-model="email" 
              class="form-input" 
              placeholder="请输入邮箱" 
              type="text"
            />
          </view>

          <view class="form-item">
            <view class="form-label">
              <svg viewBox="0 0 48 48" class="label-icon">
                <circle cx="24" cy="24" r="20" fill="#FFB6C1"/>
                <path d="M20 18 L28 18 L28 28 L20 28 Z" stroke="#FF69B4" stroke-width="2" fill="none"/>
                <path d="M20 32 L28 32" stroke="#FF69B4" stroke-width="2" fill="none"/>
              </svg>
              <text class="label-text">问题反馈</text>
            </view>
            <textarea 
              v-model="feedback" 
              class="form-textarea" 
              placeholder="写下你使用过程遇到的问题，我们会及时处理"
              :maxlength="500"
              :auto-height="true"
            />
            <text class="textarea-count">{{ feedback.length }}/500</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="footer-area">
      <view class="cancel-btn" @tap="goBack">
        <text class="cancel-text">取消</text>
      </view>
      <view class="save-btn" @tap="saveProfile">
        <svg viewBox="0 0 48 48" class="save-icon">
          <rect x="8" y="12" width="32" height="28" rx="4" fill="#FFFFFF"/>
          <path d="M16 20 L32 20" stroke="#FF69B4" stroke-width="2" fill="none"/>
          <path d="M16 26 L32 26" stroke="#FF69B4" stroke-width="2" fill="none"/>
          <path d="M16 32 L24 32" stroke="#FF69B4" stroke-width="2" fill="none"/>
        </svg>
        <text class="save-text">保存</text>
      </view>
    </view>

    <view v-if="isUploading" class="loading-overlay">
      <view class="loading-content">
        <svg viewBox="0 0 100 100" class="loading-cat">
          <circle cx="50" cy="50" r="45" fill="#FFB6C1" opacity="0.3"/>
          <circle cx="50" cy="50" r="40" fill="#FFB6C1" opacity="0.4"/>
          <circle cx="50" cy="50" r="35" fill="#FFB6C1" opacity="0.5"/>
          <circle cx="50" cy="50" r="30" fill="#FFB6C1" opacity="0.6"/>
          <circle cx="35" cy="40" r="8" fill="#FFB6C1"/>
          <circle cx="65" cy="40" r="8" fill="#FFB6C1"/>
          <circle cx="35" cy="40" r="4" fill="#333"/>
          <circle cx="65" cy="40" r="4" fill="#333"/>
          <circle cx="36" cy="39" r="1.5" fill="#fff"/>
          <circle cx="66" cy="39" r="1.5" fill="#fff"/>
          <ellipse cx="50" cy="55" rx="4" ry="3" fill="#FF69B4"/>
          <path d="M44 62 Q50 68 56 62" stroke="#333" stroke-width="2" fill="none"/>
        </svg>
        <text class="loading-text">头像上传中...</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, uploadAttachment, getAttachmentUrl } from '@/api'

const userStore = useUserStore()
const nickname = ref('')
const email = ref('')
const feedback = ref('')
const avatarFileId = ref<number | null>(null)
const tempAvatarUrl = ref('')
const isUploading = ref(false)

const avatarUrl = computed(() => {
  if (tempAvatarUrl.value) {
    return tempAvatarUrl.value
  }
  if (!userStore.userInfo?.fileIds) {
    return getDefaultAvatar()
  }
  let firstId: string | null = null
  try {
    const ids = JSON.parse(userStore.userInfo.fileIds)
    if (Array.isArray(ids) && ids.length > 0) {
      firstId = String(ids[0])
    }
  } catch {
    const parts = userStore.userInfo.fileIds.split(',')
    if (parts.length > 0) {
      firstId = parts[0].trim()
    }
  }
  return firstId ? getAttachmentUrl(Number(firstId)) : getDefaultAvatar()
})

function getDefaultAvatar(): string {
  return 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"%3E%3Ccircle cx="50" cy="50" r="45" fill="%23FFB6C1"/%3E%3Ccircle cx="38" cy="42" r="6" fill="%23333"/%3E%3Ccircle cx="62" cy="42" r="6" fill="%23333"/%3E%3Ccircle cx="39" cy="41" r="2" fill="%23fff"/%3E%3Ccircle cx="63" cy="41" r="2" fill="%23fff"/%3E%3Cellipse cx="50" cy="56" rx="5" ry="3" fill="%23FF69B4"/%3E%3Cpath d="M42 64 Q50 70 58 64" stroke="%23333" stroke-width="2" fill="none"/%3E%3Ccircle cx="28" cy="35" r="8" fill="%23FFB6C1"/%3E%3Ccircle cx="72" cy="35" r="8" fill="%23FFB6C1"/%3E%3Cpath d="M20 35 Q28 25 36 35" stroke="%23FFB6C1" stroke-width="3" fill="none"/%3E%3Cpath d="M64 35 Q72 25 80 35" stroke="%23FFB6C1" stroke-width="3" fill="none"/%3E%3C/svg%3E'
}

function validateEmail(emailStr: string): boolean {
  if (!emailStr) return true
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return regex.test(emailStr)
}

function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      if (!filePath) return

      isUploading.value = true

      try {
        const result: any = await uploadAttachment(filePath)
        if (result && result.id) {
          avatarFileId.value = result.id
          tempAvatarUrl.value = filePath
          uni.showToast({ title: '头像上传成功', icon: 'success' })
        }
      } catch (e) {
        console.error('头像上传失败:', e)
        uni.showToast({ title: '头像上传失败', icon: 'none' })
      } finally {
        isUploading.value = false
      }
    },
    fail: () => {
      uni.showToast({ title: '选择图片失败', icon: 'none' })
    }
  })
}

async function saveProfile() {
  if (!nickname.value.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }

  if (email.value && !validateEmail(email.value)) {
    uni.showToast({ title: '请输入正确的邮箱格式', icon: 'none' })
    return
  }

  const updateData: any = {
    nickname: nickname.value.trim()
  }

  if (email.value) {
    updateData.email = email.value.trim()
  }

  if (avatarFileId.value) {
    updateData.fileIds = JSON.stringify([avatarFileId.value])
  }

  try {
    await updateProfile(updateData)

    const userInfo = userStore.userInfo
    if (userInfo) {
      userStore.updateUser({
        nickname: updateData.nickname,
        email: updateData.email || userInfo.email,
        fileIds: updateData.fileIds || userInfo.fileIds
      })
    }

    uni.showToast({ title: '保存成功', icon: 'success' })

    setTimeout(() => {
      uni.navigateBack({ delta: 1 })
    }, 1500)
  } catch (e) {
    console.error('保存失败:', e)
    uni.showToast({ title: '保存失败，请重试', icon: 'none' })
  }
}

function goBack() {
  uni.navigateBack({ delta: 1 })
}

onMounted(() => {
  const user = userStore.userInfo
  if (user) {
    nickname.value = user.nickname || ''
    email.value = user.email || ''
  }
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
  padding: 24rpx;
}

.edit-card {
  background: $card-bg;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32rpx;
  padding-bottom: 32rpx;
  border-bottom: 1rpx solid rgba(255, 182, 193, 0.3);
}

.avatar-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.label-icon {
  width: 40rpx;
  height: 40rpx;
}

.label-text {
  font-size: 30rpx;
  font-weight: 600;
  color: $primary-color;
}

.avatar-preview {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  border: 6rpx solid $primary-color;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60rpx;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-icon {
  width: 36rpx;
  height: 36rpx;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.form-input {
  height: 88rpx;
  background: rgba(255, 182, 193, 0.1);
  border-radius: 24rpx;
  padding: 0 28rpx;
  font-size: 30rpx;
  color: #333;
  border: 2rpx solid rgba(255, 182, 193, 0.3);
}

.form-textarea {
  width: 100%;
  min-height: 200rpx;
  background: rgba(255, 182, 193, 0.1);
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  color: #333;
  border: 2rpx solid rgba(255, 182, 193, 0.3);
  box-sizing: border-box;
}

.textarea-count {
  text-align: right;
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
}

.footer-area {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $card-bg;
  box-shadow: 0 -8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.cancel-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
  background: rgba(255, 182, 193, 0.2);
  border-radius: 40rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.4);
  transition: all 0.2s;
}

.cancel-btn:active {
  transform: scale(0.98);
}

.cancel-text {
  font-size: 32rpx;
  font-weight: 600;
  color: $primary-color;
}

.save-btn {
  flex: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 40rpx;
  transition: all 0.2s;
}

.save-btn:active {
  transform: scale(0.98);
}

.save-icon {
  width: 40rpx;
  height: 40rpx;
}

.save-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32rpx;
}

.loading-cat {
  width: 160rpx;
  height: 160rpx;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.loading-text {
  font-size: 28rpx;
  color: #FFFFFF;
}
</style>