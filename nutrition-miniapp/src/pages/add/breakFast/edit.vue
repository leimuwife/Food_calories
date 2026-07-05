<template>
  <view class="page-container">
    <scroll-view scroll-y class="form-scroll">
      <view class="form-content">
        <view class="image-upload-area" @tap="handleImageUpload">
          <svg viewBox="0 0 200 160" class="food-image-preview">
            <rect x="10" y="10" width="180" height="140" rx="20" fill="#FFF0F3"/>
            <rect x="20" y="20" width="160" height="120" rx="15" fill="#FFB6C1"/>
            <circle cx="100" cy="60" r="20" fill="#FF69B4"/>
            <circle cx="92" cy="58" r="3" fill="#333"/>
            <circle cx="108" cy="58" r="3" fill="#333"/>
            <path d="M100 66 Q98 69 100 72 Q102 69 100 66" stroke="#333" stroke-width="1.5" fill="none"/>
            <rect x="115" y="55" width="15" height="15" rx="3" fill="#8B4513"/>
            <rect x="117" y="55" width="11" height="6" rx="1" fill="#FFD700"/>
            <text x="100" y="120" font-size="12" fill="#FF69B4" text-anchor="middle">点击替换图片</text>
          </svg>
        </view>

        <view class="form-item">
          <text class="form-label">食物名称</text>
          <input 
            v-model="formData.name" 
            class="form-input" 
            placeholder="请输入食物名称"
            placeholder-class="form-placeholder"
          />
        </view>

        <view class="form-item">
          <text class="form-label">食物描述</text>
          <textarea 
            v-model="formData.description" 
            class="form-textarea" 
            placeholder="请尽量填写食物名称 + 具体重量，例：200g 水煮西兰花、1 个全麦面包 + 250ml 纯牛奶"
            placeholder-class="form-placeholder"
            :maxlength="-1"
          />
        </view>

        <view class="form-item">
          <text class="form-label">热量</text>
          <view class="calorie-row">
            <view class="calorie-input-wrap">
              <input 
                v-model="formData.calories" 
                class="form-input calorie-input" 
                placeholder="0"
                placeholder-class="form-placeholder"
                type="digit"
              />
              <text class="calorie-unit">kcal</text>
            </view>
            <view 
              :class="['ai-btn', { 'ai-btn-disabled': !formData.description.trim() || isLoading }]" 
              @tap="handleAiEstimate"
            >
              <svg viewBox="0 0 48 48" class="ai-btn-icon">
                <circle cx="24" cy="24" r="18" fill="#FF69B4"/>
                <circle cx="18" cy="22" r="3" fill="#333"/>
                <circle cx="30" cy="22" r="3" fill="#333"/>
                <circle cx="19" cy="21" r="1" fill="#fff"/>
                <circle cx="31" cy="21" r="1" fill="#fff"/>
                <path d="M24 28 Q22 31 24 34 Q26 31 24 28" stroke="#333" stroke-width="1.5" fill="none"/>
                <rect x="32" y="16" width="8" height="8" rx="2" fill="#FFB6C1"/>
                <text x="36" y="22" font-size="4" fill="#333" text-anchor="middle">+</text>
                <rect x="32" y="26" width="8" height="8" rx="2" fill="#FFB6C1"/>
                <text x="36" y="32" font-size="4" fill="#333" text-anchor="middle">=</text>
              </svg>
              <text class="ai-btn-text">AI 估算热量</text>
            </view>
          </view>
          <text class="calorie-hint">AI 热量仅为估算参考，可手动调整真实数值</text>
        </view>
      </view>
    </scroll-view>

    <view class="bottom-btn-wrap">
      <view class="save-btn" @tap="handleSave">
        <text class="save-text">保存</text>
      </view>
    </view>

    <view v-if="isLoading" class="loading-overlay">
      <view class="loading-content">
        <svg viewBox="0 0 100 100" class="loading-cat">
          <circle cx="50" cy="50" r="30" fill="none" stroke="#FFB6C1" stroke-width="4" stroke-dasharray="150" stroke-dashoffset="0">
            <animate attributeName="stroke-dashoffset" values="0;150;0" dur="1.5s" repeatCount="indefinite"/>
          </circle>
          <circle cx="50" cy="50" r="22" fill="#FFB6C1"/>
          <circle cx="42" cy="46" r="3" fill="#333"/>
          <circle cx="58" cy="46" r="3" fill="#333"/>
          <circle cx="43" cy="45" r="1" fill="#fff"/>
          <circle cx="59" cy="45" r="1" fill="#fff"/>
          <path d="M50 54 Q48 57 50 60 Q52 57 50 54" stroke="#333" stroke-width="1.5" fill="none"/>
        </svg>
        <text class="loading-text">AI 正在计算热量...</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface FoodItem {
  id: number
  name: string
  calories: number
  description?: string
  image?: string
}

interface FormData {
  name: string
  description: string
  calories: string
}

const mode = ref<'add' | 'edit'>('add')
const isLoading = ref(false)
const formData = ref<FormData>({
  name: '',
  description: '',
  calories: '',
})

function resetForm() {
  formData.value = {
    name: '',
    description: '',
    calories: '',
  }
}

function handleImageUpload() {
  console.log('Image upload clicked')
}

async function handleAiEstimate() {
  const description = formData.value.description.trim()
  if (!description) {
    uni.showToast({
      title: '请先完善食物描述再进行 AI 估算',
      icon: 'none',
      duration: 2000,
    })
    return
  }

  if (isLoading.value) return

  isLoading.value = true

  try {
    const mockResult = await mockAiCalorieApi(description)
    
    if (mockResult && typeof mockResult === 'number') {
      if (mockResult >= 0 && mockResult <= 10000) {
        formData.value.calories = String(Math.round(mockResult))
        uni.showToast({
          title: 'AI 估算成功',
          icon: 'success',
          duration: 1500,
        })
      } else {
        throw new Error('异常数值')
      }
    } else {
      throw new Error('返回异常')
    }
  } catch (e) {
    console.error('AI estimation failed:', e)
    uni.showToast({
      title: 'AI 估算失败，请手动填写热量',
      icon: 'none',
      duration: 2000,
    })
  } finally {
    isLoading.value = false
  }
}

async function mockAiCalorieApi(description: string): Promise<number | null> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const mockValues: Record<string, number> = {
        '米饭': 116,
        '面包': 250,
        '牛奶': 54,
        '鸡蛋': 143,
        '西兰花': 34,
        '苹果': 52,
        '香蕉': 91,
      }
      
      let total = 0
      let count = 0
      for (const [key, value] of Object.entries(mockValues)) {
        if (description.includes(key)) {
          total += value
          count++
        }
      }
      
      if (count > 0) {
        resolve(total)
      } else {
        resolve(Math.floor(Math.random() * 500) + 50)
      }
    }, 1500)
  })
}

function handleSave() {
  console.log('Save clicked, mode:', mode.value, 'data:', formData.value)
  uni.navigateBack()
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = (currentPage as any).$page?.options || {}
  
  mode.value = options.mode === 'edit' ? 'edit' : 'add'
  
  if (mode.value === 'edit') {
    const foodItemStr = options.foodItem
    if (foodItemStr) {
      try {
        const foodItem: FoodItem = JSON.parse(decodeURIComponent(foodItemStr))
        formData.value = {
          name: foodItem.name,
          description: foodItem.description || '',
          calories: String(foodItem.calories),
        }
      } catch (e) {
        console.error('Parse food item error:', e)
      }
    }
  } else {
    resetForm()
  }
})
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF9FA 0%, #FFF5F7 100%);
  padding-bottom: 160rpx;
  position: relative;
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

.form-scroll {
  height: calc(100vh - 280rpx);
  padding: 32rpx;
}

.form-content {
  padding-right: 8rpx;
}

.image-upload-area {
  margin-bottom: 32rpx;
}

.food-image-preview {
  width: 100%;
  height: 320rpx;
}

.form-item {
  margin-bottom: 28rpx;
}

.form-label {
  font-size: 28rpx;
  color: #FF69B4;
  font-weight: 500;
  margin-bottom: 16rpx;
  display: block;
}

.form-input {
  width: 100%;
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 28rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.4);
  font-size: 30rpx;
  color: #555555;
  box-shadow: 0 4rpx 12rpx rgba(255, 182, 193, 0.08);
}

.form-textarea {
  width: 100%;
  padding: 24rpx;
  background: #FFFFFF;
  border-radius: 28rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.4);
  font-size: 30rpx;
  color: #555555;
  height: 200rpx;
  box-shadow: 0 4rpx 12rpx rgba(255, 182, 193, 0.08);
}

.form-placeholder {
  color: #B8B8B8;
}

.calorie-row {
  display: flex;
  gap: 20rpx;
}

.calorie-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border-radius: 28rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.4);
  box-shadow: 0 4rpx 12rpx rgba(255, 182, 193, 0.08);
  padding: 0 24rpx;
}

.calorie-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 24rpx 0;
  font-size: 30rpx;
  color: #555555;
}

.calorie-unit {
  font-size: 28rpx;
  color: #FF69B4;
  font-weight: 500;
}

.calorie-hint {
  font-size: 24rpx;
  color: rgba(255, 105, 180, 0.6);
  margin-top: 12rpx;
  display: block;
}

.ai-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20rpx 28rpx;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  border-radius: 28rpx;
  box-shadow: 0 8rpx 20rpx rgba(255, 105, 180, 0.25);
  min-width: 200rpx;
}

.ai-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
  background: #E8E8E8;
  box-shadow: none;
}

.ai-btn-icon {
  width: 56rpx;
  height: 56rpx;
  margin-bottom: 8rpx;
}

.ai-btn-text {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 500;
  white-space: nowrap;
}

.bottom-btn-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255, 249, 250, 0.95);
  backdrop-filter: blur(20rpx);
}

.save-btn {
  padding: 28rpx;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  border-radius: 32rpx;
  box-shadow: 0 8rpx 20rpx rgba(255, 105, 180, 0.25);
}

.save-text {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 600;
  text-align: center;
  display: block;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 249, 250, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24rpx;
}

.loading-cat {
  width: 160rpx;
  height: 160rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #FF69B4;
}
</style>
