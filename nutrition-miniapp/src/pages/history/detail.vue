<template>
  <view class="page-container">
    <view class="header-nav">
      <view class="back-btn" @tap="goBack">
        <svg viewBox="0 0 48 48" class="back-icon">
          <circle cx="24" cy="24" r="20" fill="#FF69B4"/>
          <path d="M30 18 L20 24 L30 30" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </view>
      <text class="nav-title">{{ pageTitle }}</text>
      <view class="nav-placeholder"></view>
    </view>

    <view class="date-filter">
      <text class="filter-label">选择日期</text>
      <picker mode="date" :value="selectedDate" @change="onDateChange">
        <view class="date-picker">
          <text class="picker-text">{{ selectedDate }}</text>
          <svg viewBox="0 0 48 48" class="picker-icon">
            <rect x="8" y="8" width="32" height="32" rx="8" fill="#FF69B4"/>
            <text x="24" y="30" text-anchor="middle" fill="#fff" font-size="16" font-weight="bold">▼</text>
          </svg>
        </view>
      </picker>
    </view>

    <scroll-view 
      scroll-y 
      class="page-scroll"
      @refresherrefresh="onRefresh"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
    >
      <view v-if="dietItems.length > 0" class="food-list">
        <view v-for="item in dietItems" :key="item.id" class="food-card">
          <view class="food-image" @tap="previewImage(item)">
            <image 
              v-if="item.imageUrl" 
              :src="item.imageUrl" 
              class="food-img" 
              mode="aspectFill"
            />
            <view v-else class="food-placeholder">
              <svg viewBox="0 0 80 80" class="placeholder-icon">
                <rect x="10" y="10" width="60" height="60" rx="12" fill="#FFB6C1" opacity="0.3"/>
                <circle cx="30" cy="30" r="8" fill="#FF69B4" opacity="0.5"/>
                <rect x="38" y="26" width="24" height="8" rx="2" fill="#FF69B4" opacity="0.5"/>
                <rect x="38" y="40" width="32" height="6" rx="2" fill="#FF69B4" opacity="0.3"/>
              </svg>
            </view>
          </view>
          <view class="food-info">
            <text class="food-name">{{ item.foodName }}</text>
            <view class="food-detail">
              <text class="food-weight">{{ item.weight }}g</text>
              <text class="food-calories">{{ item.calories }} kcal</text>
            </view>
            <text class="food-desc">{{ getFoodDesc(item) }}</text>
          </view>
        </view>
      </view>

      <view v-if="dietItems.length === 0 && !isLoading" class="empty-state">
        <svg viewBox="0 0 120 120" class="empty-icon">
          <circle cx="60" cy="60" r="50" fill="#FFB6C1" opacity="0.3"/>
          <text x="60" y="55" text-anchor="middle" fill="#FF69B4" font-size="36">🍽️</text>
          <text x="60" y="85" text-anchor="middle" fill="#999" font-size="14">暂无记录</text>
        </svg>
        <text class="empty-text">该时段暂无饮食记录</text>
      </view>
    </scroll-view>

    <view v-if="dietItems.length > 0" class="summary-bar">
      <text class="summary-label">{{ mealName }}总摄入</text>
      <view class="summary-value">
        <text class="summary-calories">{{ totalCalories }}</text>
        <text class="summary-unit">kcal</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTodayRecords } from '@/api/history/history'
import type { DailyDietVO, DietItemVO } from '@/api/types'

interface DietItemWithImage extends DietItemVO {
  imageUrl?: string
}

const selectedDate = ref('')
const mealType = ref('breakfast')
const isLoading = ref(false)
const isRefreshing = ref(false)
const dietItems = ref<DietItemWithImage[]>([])

const mealNames: Record<string, string> = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐',
  snack: '夜宵',
}

const mealName = computed(() => mealNames[mealType.value] || '')

const pageTitle = computed(() => {
  return `${selectedDate.value} ${mealName.value}记录`
})

const totalCalories = computed(() => {
  return dietItems.value.reduce((sum, item) => sum + item.calories, 0)
})

function getTodayDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function goBack() {
  uni.navigateBack()
}

function getFoodDesc(item: DietItemWithImage) {
  return item.foodDesc || item.remark || ''
}

function onDateChange(e: { detail: { value: string } }) {
  selectedDate.value = e.detail.value
  loadData()
}

function previewImage(item: DietItemWithImage) {
  if (item.imageUrl) {
    uni.previewImage({
      urls: [item.imageUrl],
      current: item.imageUrl
    })
  }
}

async function loadData() {
  isLoading.value = true
  try {
    const res = await getTodayRecords(selectedDate.value)
    const data = res.data as DailyDietVO
    
    const mealItems = data.summary.meals[mealType.value]?.items || []
    dietItems.value = mealItems.map(item => ({
      ...item,
      imageUrl: ''
    }))
  } catch (e) {
    console.error('加载数据失败:', e)
    dietItems.value = []
  } finally {
    isLoading.value = false
    isRefreshing.value = false
  }
}

async function onRefresh() {
  await loadData()
}

onMounted(() => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const options = (currentPage as any).$page?.options || {}
  
  selectedDate.value = options.date || getTodayDate()
  mealType.value = options.mealType || 'breakfast'
  
  loadData()
})
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;
$light-pink: #FFB6C1;
$bg-color: #FFF9FA;
$card-bg: #FFFFFF;

.page-container {
  min-height: 100vh;
  background: $bg-color;
  padding-bottom: 140rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 24rpx 24rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
}

.back-btn {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.back-btn:active {
  transform: scale(0.95);
}

.back-icon {
  width: 56rpx;
  height: 56rpx;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.nav-placeholder {
  width: 72rpx;
}

.date-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  background: $card-bg;
  box-shadow: 0 4rpx 16rpx rgba(255, 182, 193, 0.1);
}

.filter-label {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.date-picker {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 20rpx;
  background: rgba(255, 105, 180, 0.1);
  border-radius: 20rpx;
}

.picker-text {
  font-size: 26rpx;
  color: $primary-color;
  font-weight: 500;
}

.picker-icon {
  width: 28rpx;
  height: 28rpx;
}

.page-scroll {
  height: calc(100vh - 260rpx);
}

.food-list {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.food-card {
  display: flex;
  background: $card-bg;
  border-radius: 28rpx;
  padding: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 182, 193, 0.1);
}

.food-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 20rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.food-img {
  width: 100%;
  height: 100%;
}

.food-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 182, 193, 0.1);
}

.placeholder-icon {
  width: 60rpx;
  height: 60rpx;
}

.food-info {
  flex: 1;
  padding: 8rpx 16rpx;
  display: flex;
  flex-direction: column;
}

.food-name {
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.food-detail {
  display: flex;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.food-weight {
  font-size: 24rpx;
  color: #999;
}

.food-calories {
  font-size: 24rpx;
  color: $primary-color;
  font-weight: 500;
}

.food-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.summary-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $card-bg;
  box-shadow: 0 -4rpx 16rpx rgba(255, 182, 193, 0.1);
}

.summary-label {
  font-size: 28rpx;
  color: #666;
}

.summary-value {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.summary-calories {
  font-size: 48rpx;
  color: $primary-color;
  font-weight: 700;
}

.summary-unit {
  font-size: 24rpx;
  color: #999;
}
</style>