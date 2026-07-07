<template>
  <view class="page-container">
    <view class="header-nav">
      <view class="back-btn" @tap="goBack">
        <svg viewBox="0 0 48 48" class="back-icon">
          <circle cx="24" cy="24" r="20" fill="#FF69B4"/>
          <path d="M30 18 L20 24 L30 30" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </view>
      <text class="nav-title">饮食历史记录</text>
      <view class="nav-placeholder"></view>
    </view>

    <view class="date-filter">
      <text class="filter-label">选择查询日期</text>
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
      <view class="meal-cards">
        <view 
          v-for="meal in mealList" 
          :key="meal.key" 
          class="meal-card"
          @click="goToDetail(meal.key)"
        >
          <view class="meal-icon-wrap" :style="{ background: meal.color }">
            <view v-if="meal.key === 'breakfast'" class="meal-icon-inner breakfast-icon">
              <view class="sun"></view>
              <view class="bread"></view>
            </view>
            <view v-else-if="meal.key === 'lunch'" class="meal-icon-inner lunch-icon">
              <view class="bowl"></view>
              <view class="rice"></view>
            </view>
            <view v-else-if="meal.key === 'dinner'" class="meal-icon-inner dinner-icon">
              <view class="moon"></view>
              <view class="plate"></view>
            </view>
            <view v-else class="meal-icon-inner snack-icon">
              <view class="star"></view>
              <view class="cake"></view>
            </view>
          </view>
          <text class="meal-name">{{ meal.name }}</text>
          <text class="meal-calories">{{ getMealCalories(meal.key) }} kcal</text>
        </view>
      </view>

      <view v-if="dailyData" class="chart-section">
        <text class="section-title">本月四大餐段热量占比</text>
        <view class="chart-container">
          <view class="pie-chart">
            <view class="pie-circle" :style="pieStyle"></view>
            <view class="pie-center">
              <text class="pie-total">{{ totalMonthlyCalories }}</text>
              <text class="pie-unit">kcal</text>
            </view>
          </view>
          <view class="chart-legend">
            <view v-for="(meal, idx) in mealList" :key="meal.key" class="legend-item">
              <view class="legend-dot" :style="{ background: meal.color }"></view>
              <text class="legend-text">{{ meal.name }}</text>
              <text class="legend-value">{{ getMonthlyMealCalories(meal.key) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="!dailyData && !isLoading" class="empty-state">
        <view class="empty-icon">
          <text class="empty-emoji">🍽️</text>
        </view>
        <text class="empty-text">当前日期暂无饮食记录</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTodayRecords, getMonthlySummary } from '@/api/history/history'
import type { DailyDietVO, MonthlySummaryVO } from '@/api/types'

const selectedDate = ref('')
const isLoading = ref(false)
const isRefreshing = ref(false)
const dailyData = ref<DailyDietVO | null>(null)
const monthlyData = ref<MonthlySummaryVO | null>(null)

const mealList = [
  { key: 'breakfast', name: '早餐', color: '#FF69B4' },
  { key: 'lunch', name: '午餐', color: '#FF8C42' },
  { key: 'dinner', name: '晚餐', color: '#42A5F5' },
  { key: 'snack', name: '夜宵', color: '#9575CD' },
]

const currentMonth = computed(() => {
  const parts = selectedDate.value.split('-')
  return { year: parseInt(parts[0]), month: parseInt(parts[1]) }
})

const totalMonthlyCalories = computed(() => {
  if (!dailyData.value) return 0
  return dailyData.value.summary.totalCalories
})

const pieStyle = computed(() => {
  if (!dailyData.value) return {}
  const summary = dailyData.value.summary
  const breakfast = summary.meals['breakfast']?.calories || 0
  const lunch = summary.meals['lunch']?.calories || 0
  const dinner = summary.meals['dinner']?.calories || 0
  const snack = summary.meals['snack']?.calories || 0
  const total = breakfast + lunch + dinner + snack
  
  if (total === 0) {
    return { background: 'conic-gradient(#FFB6C1 0deg, #FFB6C1 360deg)' }
  }
  
  const angle1 = (breakfast / total) * 360
  const angle2 = (lunch / total) * 360
  const angle3 = (dinner / total) * 360
  
  return {
    background: `conic-gradient(#FF69B4 0deg, #FF69B4 ${angle1}deg, #FF8C42 ${angle1}deg, #FF8C42 ${angle1 + angle2}deg, #42A5F5 ${angle1 + angle2}deg, #42A5F5 ${angle1 + angle2 + angle3}deg, #9575CD ${angle1 + angle2 + angle3}deg, #9575CD 360deg)`
  }
})

function getTodayDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getMealCalories(mealType: string) {
  if (!dailyData.value) return 0
  return dailyData.value.summary.meals[mealType]?.calories || 0
}

function getMonthlyMealCalories(mealType: string) {
  if (!dailyData.value) return 0
  return dailyData.value.summary.meals[mealType]?.calories || 0
}

function goBack() {
  uni.navigateBack()
}

function onDateChange(e: { detail: { value: string } }) {
  selectedDate.value = e.detail.value
  loadData()
}

function goToDetail(mealType: string) {
  uni.navigateTo({
    url: `/pages/history/detail?date=${selectedDate.value}&mealType=${mealType}`
  })
}

async function loadData() {
  isLoading.value = true
  try {
    const [dailyRes, monthlyRes] = await Promise.all([
      getTodayRecords(selectedDate.value),
      getMonthlySummary(currentMonth.value.year, currentMonth.value.month)
    ])
    dailyData.value = dailyRes.data
    monthlyData.value = monthlyRes.data
  } catch (e) {
    console.error('加载数据失败:', e)
    dailyData.value = null
    monthlyData.value = null
  } finally {
    isLoading.value = false
    isRefreshing.value = false
  }
}

async function onRefresh() {
  selectedDate.value = getTodayDate()
  await loadData()
}

onMounted(() => {
  selectedDate.value = getTodayDate()
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
  font-size: 36rpx;
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
  padding: 32rpx;
  background: $card-bg;
  margin: 24rpx;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 182, 193, 0.1);
}

.filter-label {
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
}

.date-picker {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 24rpx;
  background: rgba(255, 105, 180, 0.1);
  border-radius: 24rpx;
}

.picker-text {
  font-size: 28rpx;
  color: $primary-color;
  font-weight: 500;
}

.picker-icon {
  width: 32rpx;
  height: 32rpx;
}

.page-scroll {
  height: calc(100vh - 260rpx);
}

.meal-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  padding: 0 24rpx;
}

.meal-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 16rpx;
  background: $card-bg;
  border-radius: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 182, 193, 0.1);
  transition: transform 0.2s;
  cursor: pointer;
}

.meal-card:active {
  transform: scale(0.95);
}

.meal-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.meal-icon-inner {
  width: 48rpx;
  height: 48rpx;
  position: relative;
}

.breakfast-icon .sun {
  width: 24rpx;
  height: 24rpx;
  background: #FFD700;
  border-radius: 50%;
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.breakfast-icon .bread {
  width: 32rpx;
  height: 16rpx;
  background: #DEB887;
  border-radius: 4rpx;
  position: absolute;
  bottom: 4rpx;
  left: 50%;
  transform: translateX(-50%);
}

.lunch-icon .bowl {
  width: 36rpx;
  height: 20rpx;
  background: #fff;
  border-radius: 0 0 18rpx 18rpx;
  position: absolute;
  bottom: 8rpx;
  left: 50%;
  transform: translateX(-50%);
}

.lunch-icon .rice {
  width: 24rpx;
  height: 8rpx;
  background: #fff;
  border-radius: 4rpx;
  position: absolute;
  bottom: 14rpx;
  left: 50%;
  transform: translateX(-50%);
}

.dinner-icon .moon {
  width: 24rpx;
  height: 24rpx;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.dinner-icon .moon::before {
  content: '';
  width: 10rpx;
  height: 10rpx;
  background: currentColor;
  border-radius: 50%;
  position: absolute;
  top: 4rpx;
  right: 4rpx;
}

.dinner-icon .plate {
  width: 36rpx;
  height: 8rpx;
  background: #fff;
  border-radius: 4rpx;
  position: absolute;
  bottom: 8rpx;
  left: 50%;
  transform: translateX(-50%);
}

.snack-icon .star {
  width: 0;
  height: 0;
  border-left: 10rpx solid transparent;
  border-right: 10rpx solid transparent;
  border-bottom: 20rpx solid #FFD700;
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
}

.snack-icon .cake {
  width: 28rpx;
  height: 16rpx;
  background: #fff;
  border-radius: 4rpx;
  position: absolute;
  bottom: 8rpx;
  left: 50%;
  transform: translateX(-50%);
}

.meal-name {
  font-size: 24rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.meal-calories {
  font-size: 22rpx;
  color: $primary-color;
  font-weight: 600;
}

.chart-section {
  margin-top: 32rpx;
  padding: 32rpx;
  background: $card-bg;
  margin: 32rpx 24rpx;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 182, 193, 0.1);
}

.section-title {
  font-size: 32rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 32rpx;
  display: block;
}

.chart-container {
  display: flex;
  align-items: center;
  gap: 32rpx;
}

.pie-chart {
  position: relative;
  width: 280rpx;
  height: 280rpx;
  flex-shrink: 0;
}

.pie-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 160rpx;
  height: 160rpx;
  background: $card-bg;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.pie-total {
  font-size: 36rpx;
  color: $primary-color;
  font-weight: 700;
}

.pie-unit {
  font-size: 20rpx;
  color: #999;
}

.chart-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.legend-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 4rpx;
}

.legend-text {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.legend-value {
  font-size: 26rpx;
  color: #999;
  font-weight: 500;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  background: rgba(255, 182, 193, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}

.empty-emoji {
  font-size: 60rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>