<template>
  <view class="page-container">
    <view class="header-area">
      <view class="back-btn" @tap="goBack">
        <svg viewBox="0 0 48 48" class="back-icon">
          <circle cx="24" cy="24" r="22" fill="#FFB6C1"/>
          <path d="M28 18 L20 24 L28 30" stroke="#FF69B4" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="18" cy="24" r="3" fill="#FF69B4"/>
          <circle cx="19" cy="23" r="1" fill="#fff"/>
        </svg>
      </view>
      <text class="header-title">发布轻友圈动态</text>
      <view class="placeholder"></view>
    </view>

    <scroll-view scroll-y class="content-scroll">
      <view class="edit-card">
        <textarea 
          v-model="content" 
          class="content-textarea"
          placeholder="分享你的减肥日常、饮食打卡..."
          :maxlength="500"
          :auto-height="true"
          @input="onContentInput"
        />
        <view class="word-count">
          <text class="word-count-text">{{ content.length }}/500</text>
        </view>
      </view>

      <view class="image-card">
        <view class="image-card-header">
          <svg viewBox="0 0 48 48" class="image-icon">
            <rect x="8" y="8" width="32" height="28" rx="4" fill="#FFB6C1"/>
            <circle cx="20" cy="18" r="4" fill="#FF69B4"/>
            <circle cx="32" cy="22" r="3" fill="#FF69B4"/>
            <rect x="12" y="26" width="8" height="4" rx="1" fill="#FF69B4"/>
          </svg>
          <text class="image-card-title">图片</text>
        </view>

        <scroll-view scroll-x class="image-scroll">
          <view class="image-list">
            <view 
              v-for="(img, idx) in uploadedImages" 
              :key="idx" 
              class="image-item"
            >
              <image :src="img.url" class="uploaded-image" mode="aspectFill"/>
              <view class="image-delete" @tap="deleteImage(idx)">
                <svg viewBox="0 0 48 48" class="delete-icon">
                  <circle cx="24" cy="24" r="18" fill="#FF69B4"/>
                  <path d="M18 18 L30 30 M30 18 L18 30" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round"/>
                </svg>
              </view>
            </view>

            <view v-if="uploadedImages.length < 9" class="image-add-btn" @tap="chooseImage">
              <svg viewBox="0 0 48 48" class="add-icon">
                <rect x="8" y="8" width="32" height="32" rx="8" fill="rgba(255, 182, 193, 0.3)"/>
                <path d="M24 18 L24 30 M18 24 L30 24" stroke="#FF69B4" stroke-width="3" fill="none" stroke-linecap="round"/>
              </svg>
              <text class="add-text">添加图片</text>
            </view>
          </view>
        </scroll-view>

        <view v-if="uploadedImages.length > 0" class="image-hint">
          <text class="hint-text">点击图片可删除，最多上传9张</text>
        </view>
      </view>
    </scroll-view>

    <view class="footer-area">
      <view class="save-draft-btn" @tap="saveDraft">
        <svg viewBox="0 0 48 48" class="draft-icon">
          <rect x="8" y="12" width="32" height="28" rx="4" fill="#FFB6C1"/>
          <path d="M16 20 L32 20" stroke="#FF69B4" stroke-width="2" fill="none"/>
          <path d="M16 26 L32 26" stroke="#FF69B4" stroke-width="2" fill="none"/>
          <path d="M16 32 L24 32" stroke="#FF69B4" stroke-width="2" fill="none"/>
        </svg>
        <text class="save-draft-text">保存草稿</text>
      </view>
      <view :class="['publish-btn', { 'publish-btn-disabled': !canPublish }]" @tap="handlePublish">
        <svg viewBox="0 0 48 48" class="publish-icon">
          <circle cx="24" cy="24" r="20" fill="#FF69B4"/>
          <path d="M14 24 L24 14 L34 24" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="24" cy="24" r="4" fill="#fff"/>
        </svg>
        <text class="publish-text">发布</text>
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
        <text class="loading-text">图片上传中...</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { publishFeed, uploadAttachment } from '@/api'

interface UploadedImage {
  fileId: number
  url: string
}

const content = ref('')
const uploadedImages = ref<UploadedImage[]>([])
const isUploading = ref(false)

const DRAFT_KEY = 'qingyouquan_draft'

const canPublish = computed(() => {
  return content.value.trim().length > 0 || uploadedImages.value.length > 0
})

function onContentInput() {}

function chooseImage() {
  uni.chooseImage({
    count: 9 - uploadedImages.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePaths = res.tempFilePaths
      if (tempFilePaths.length === 0) return

      isUploading.value = true

      try {
        for (const filePath of tempFilePaths) {
          const result: any = await uploadAttachment(filePath)
          if (result && result.id) {
            uploadedImages.value.push({
              fileId: result.id,
              url: filePath
            })
          }
        }
        uni.showToast({ title: '上传成功', icon: 'success' })
      } catch (e) {
        console.error('上传失败:', e)
        uni.showToast({ title: '部分图片上传失败', icon: 'none' })
      } finally {
        isUploading.value = false
      }
    },
    fail: () => {
      uni.showToast({ title: '选择图片失败', icon: 'none' })
    }
  })
}

function deleteImage(index: number) {
  uploadedImages.value.splice(index, 1)
}

function saveDraft() {
  if (!content.value.trim() && uploadedImages.value.length === 0) {
    uni.showToast({ title: '没有可保存的内容', icon: 'none' })
    return
  }

  const draft = {
    content: content.value,
    fileIds: uploadedImages.value.map(img => img.fileId),
    updateTime: Date.now()
  }

  uni.setStorageSync(DRAFT_KEY, JSON.stringify(draft))
  uni.showToast({ title: '草稿已保存', icon: 'success' })
}

async function handlePublish() {
  if (!canPublish.value) return

  const fileIds = uploadedImages.value.map(img => img.fileId)

  try {
    await publishFeed({
      content: content.value.trim(),
      fileIds
    })

    uni.removeStorageSync(DRAFT_KEY)
    uni.showToast({ title: '发布成功', icon: 'success' })

    setTimeout(() => {
      uni.navigateBack({
        delta: 1
      })
    }, 1500)
  } catch (e) {
    console.error('发布失败:', e)
    uni.showToast({ title: '发布失败，请重试', icon: 'none' })
  }
}

function goBack() {
  if (content.value.trim() || uploadedImages.value.length > 0) {
    uni.showModal({
      title: '提示',
      content: '是否保存草稿？',
      confirmText: '保存',
      cancelText: '不保存',
      success: (res) => {
        if (res.confirm) {
          saveDraft()
        }
        uni.navigateBack({ delta: 1 })
      }
    })
  } else {
    uni.navigateBack({ delta: 1 })
  }
}

function loadDraft() {
  try {
    const draftStr = uni.getStorageSync(DRAFT_KEY)
    if (draftStr) {
      const draft = JSON.parse(draftStr)
      content.value = draft.content || ''
    }
  } catch (e) {
    console.error('加载草稿失败:', e)
  }
}

onMounted(() => {
  loadDraft()
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
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.content-textarea {
  width: 100%;
  min-height: 320rpx;
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
  background: transparent;
}

.word-count {
  text-align: right;
  margin-top: 16rpx;
}

.word-count-text {
  font-size: 24rpx;
  color: #999;
}

.image-card {
  background: $card-bg;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.image-card-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.image-icon {
  width: 48rpx;
  height: 48rpx;
}

.image-card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $primary-color;
}

.image-scroll {
  white-space: nowrap;
}

.image-list {
  display: inline-flex;
  gap: 20rpx;
}

.image-item {
  width: 200rpx;
  height: 200rpx;
  border-radius: 24rpx;
  overflow: hidden;
  position: relative;
}

.uploaded-image {
  width: 100%;
  height: 100%;
}

.image-delete {
  position: absolute;
  top: -20rpx;
  right: -20rpx;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-icon {
  width: 48rpx;
  height: 48rpx;
}

.image-add-btn {
  width: 200rpx;
  height: 200rpx;
  border-radius: 24rpx;
  background: rgba(255, 182, 193, 0.15);
  border: 3rpx dashed rgba(255, 182, 193, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  transition: all 0.2s;
}

.image-add-btn:active {
  transform: scale(0.95);
  background: rgba(255, 182, 193, 0.25);
}

.add-icon {
  width: 64rpx;
  height: 64rpx;
}

.add-text {
  font-size: 24rpx;
  color: $primary-color;
}

.image-hint {
  margin-top: 20rpx;
  text-align: center;
}

.hint-text {
  font-size: 24rpx;
  color: #999;
}

.footer-area {
  display: flex;
  gap: 24rpx;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $card-bg;
  box-shadow: 0 -8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.save-draft-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 28rpx;
  background: rgba(255, 182, 193, 0.2);
  border-radius: 40rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.4);
  transition: all 0.2s;
}

.save-draft-btn:active {
  transform: scale(0.98);
  background: rgba(255, 182, 193, 0.3);
}

.draft-icon {
  width: 48rpx;
  height: 48rpx;
}

.save-draft-text {
  font-size: 30rpx;
  font-weight: 600;
  color: $primary-color;
}

.publish-btn {
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

.publish-btn:active {
  transform: scale(0.98);
}

.publish-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}

.publish-icon {
  width: 48rpx;
  height: 48rpx;
}

.publish-text {
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