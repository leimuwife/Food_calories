<template>
  <view class="page-container">
    <scroll-view scroll-y class="form-scroll">
      <view class="form-content">
        <view class="image-upload-area" @tap="handleImageUpload">
          <image v-if="imageUrl" :src="imageUrl" class="food-image-preview" mode="aspectFill"/>
          <svg v-else viewBox="0 0 200 160" class="food-image-preview">
            <rect x="10" y="10" width="180" height="140" rx="20" fill="#FFF0F3"/>
            <rect x="20" y="20" width="160" height="120" rx="15" fill="#FFB6C1"/>
            <circle cx="100" cy="60" r="20" fill="#FF69B4"/>
            <circle cx="92" cy="58" r="3" fill="#333"/>
            <circle cx="108" cy="58" r="3" fill="#333"/>
            <path d="M100 66 Q98 69 100 72 Q102 69 100 66" stroke="#333" stroke-width="1.5" fill="none"/>
            <rect x="115" y="55" width="15" height="15" rx="3" fill="#8B4513"/>
            <rect x="117" y="55" width="11" height="6" rx="1" fill="#FFD700"/>
            <text x="100" y="120" font-size="12" fill="#FF69B4" text-anchor="middle">点击上传图片</text>
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
          <text class="form-label">食用重量</text>
          <view class="weight-input-wrap">
            <input 
              v-model="formData.weight" 
              class="form-input weight-input" 
              placeholder="0"
              placeholder-class="form-placeholder"
              type="digit"
            />
            <text class="weight-unit">g</text>
          </view>
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

        <view class="form-item">
          <text class="form-label">备注</text>
          <textarea 
            v-model="formData.remark" 
            class="form-textarea" 
            placeholder="添加备注（选填）"
            placeholder-class="form-placeholder"
            :maxlength="-1"
          />
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
import { estimateCalories, addDietRecord } from '@/api/add/add'
import type { MealType } from '@/api/types'

interface FoodItem {
  id: number
  name: string
  calories: number
  description?: string
  weight?: number
  remark?: string
}

interface FormData {
  name: string
  description: string
  weight: string
  calories: string
  remark: string
}

const mode = ref<'add' | 'edit'>('add')
const isLoading = ref(false)
const formData = ref<FormData>({
  name: '',
  description: '',
  weight: '',
  calories: '',
  remark: '',
})
const imageUrl = ref('')
const tempFilePath = ref('')

function resetForm() {
  formData.value = {
    name: '',
    description: '',
    weight: '',
    calories: '',
    remark: '',
  }
  imageUrl.value = ''
  tempFilePath.value = ''
}

function handleImageUpload() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      tempFilePath.value = res.tempFilePaths[0]
      imageUrl.value = res.tempFilePaths[0]
    },
    fail: () => {
      uni.showToast({ title: '取消选择', icon: 'none' })
    }
  })
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
    const res = await estimateCalories(description)
    const calories = res.data.calories
    
    if (calories >= 0 && calories <= 10000) {
      formData.value.calories = String(Math.round(calories))
      uni.showToast({
        title: 'AI 估算成功',
        icon: 'success',
        duration: 1500,
      })
    } else {
      throw new Error('异常数值')
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

async function handleSave() {
  if (isLoading.value) return
  
  const name = formData.value.name.trim()
  const calories = Number(formData.value.calories)
  
  if (!name) {
    uni.showToast({ title: '请输入食物名称', icon: 'none' })
    return
  }
  
  if (!calories || calories <= 0) {
    uni.showToast({ title: '请输入热量数值', icon: 'none' })
    return
  }

  isLoading.value = true

  try {
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    const mealType = (prevPage as any).$page?.route?.split('/').pop()?.replace('index', '') as MealType || 'snack'
    
    const today = new Date()
    const recordDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

    const result = await addDietRecord({
      recordDate,
      mealType,
      items: [{
        foodName: name,
        foodDesc: formData.value.description.trim() || undefined,
        weight: Number(formData.value.weight) || 0,
        calories,
        remark: formData.value.remark.trim() || undefined,
      }],
    }, tempFilePath.value)

    if (result && result.recordId) {
      uni.showToast({ title: '保存成功', icon: 'success' })
      uni.$emit('dietUpdated')
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    } else {
      uni.showToast({ title: '保存失败', icon: 'none' })
    }
  } catch (e) {
    console.error('Save failed:', e)
  } finally {
    isLoading.value = false
  }
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
          weight: String(foodItem.weight || ''),
          calories: String(foodItem.calories),
          remark: foodItem.remark || '',
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
  height: calc(100vh - 160rpx);
}

.form-content {
  padding: 32rpx;
}

.image-upload-area {
  margin-bottom: 32rpx;
  position: relative;
}

.food-image-preview {
  width: 100%;
  height: 320rpx;
  border-radius: 24rpx;
}

.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
}

.upload-text {
  color: #fff;
  font-size: 28rpx;
}

.form-item {
  margin-bottom: 32rpx;
}

.form-label {
  display: block;
  font-size: 28rpx;
  color: #FF69B4;
  font-weight: 500;
  margin-bottom: 16rpx;
}

.form-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  background: #FFFFFF;
  border: 2rpx solid #FFB6C1;
  border-radius: 24rpx;
  font-size: 30rpx;
  color: #333;
  box-sizing: border-box;
}

.form-placeholder {
  color: #CCC;
}

.form-textarea {
  width: 100%;
  height: 200rpx;
  padding: 24rpx;
  background: #FFFFFF;
  border: 2rpx solid #FFB6C1;
  border-radius: 24rpx;
  font-size: 30rpx;
  color: #333;
  box-sizing: border-box;
}

.weight-input-wrap {
  display: flex;
  align-items: center;
}

.weight-input {
  flex: 1;
}

.weight-unit {
  margin-left: -60rpx;
  font-size: 28rpx;
  color: #FF69B4;
  font-weight: 500;
}

.calorie-row {
  display: flex;
  gap: 24rpx;
}

.calorie-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
}

.calorie-input {
  flex: 1;
}

.calorie-unit {
  margin-left: -60rpx;
  font-size: 28rpx;
  color: #FF69B4;
  font-weight: 500;
}

.ai-btn {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 32rpx;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 105, 180, 0.3);
  transition: transform 0.2s;
}

.ai-btn:active {
  transform: scale(0.95);
}

.ai-btn-disabled {
  background: #DDD;
  box-shadow: none;
  opacity: 0.6;
}

.ai-btn-icon {
  width: 40rpx;
  height: 40rpx;
}

.ai-btn-text {
  font-size: 26rpx;
  color: #FFFFFF;
  font-weight: 500;
}

.calorie-hint {
  display: block;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: #FFB6C1;
}

.bottom-btn-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255, 249, 250, 0.95);
  backdrop-filter: blur(10px);
}

.save-btn {
  height: 96rpx;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 105, 180, 0.3);
  transition: transform 0.2s;
}

.save-btn:active {
  transform: scale(0.98);
}

.save-text {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 600;
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
  z-index: 1000;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.loading-cat {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 24rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #FF69B4;
}
</style>