<template>
  <view class="page-container">
    <view class="header-card">
      <view class="header-content">
        <text class="header-title">午餐</text>
        <view class="checkin-icon-wrap">
          <svg viewBox="0 0 64 64" class="checkin-icon">
            <circle cx="32" cy="32" r="28" fill="#FFA07A"/>
            <circle cx="24" cy="28" r="4" fill="#333"/>
            <circle cx="40" cy="28" r="4" fill="#333"/>
            <circle cx="25" cy="27" r="1.5" fill="#fff"/>
            <circle cx="41" cy="27" r="1.5" fill="#fff"/>
            <path d="M32 36 Q30 40 32 44 Q34 40 32 36" stroke="#333" stroke-width="2" fill="none"/>
            <rect x="38" y="18" width="14" height="12" rx="2" fill="#FF6347"/>
            <text x="45" y="27" font-size="8" fill="#fff" text-anchor="middle">20</text>
          </svg>
        </view>
      </view>
      <view class="date-picker-wrap">
        <picker mode="date" :value="selectedDate" @change="handleDateChange">
          <view class="date-picker-btn">
            <text class="date-icon">📅</text>
            <text class="date-text">{{ formatDate(selectedDate) }}</text>
            <text class="date-arrow">▼</text>
          </view>
        </picker>
      </view>
      <view class="calorie-summary">
        <text class="summary-label">{{ getDateLabel() }}午餐总热量</text>
        <text class="summary-value">{{ totalCalories }}</text>
        <text class="summary-unit">kcal</text>
      </view>
    </view>

    <scroll-view scroll-y class="food-scroll">
      <view v-if="foodList.length > 0" class="food-list">
        <view v-for="item in foodList" :key="item.id" class="food-card" @tap="handleEdit(item)">
          <view class="food-image">
            <image v-if="item.imageUrls && item.imageUrls.length > 0" :src="item.imageUrls[0]" class="food-photo" mode="aspectFill"/>
            <svg v-else viewBox="0 0 64 64" class="food-icon">
              <circle cx="32" cy="32" r="24" fill="#FFA07A"/>
              <circle cx="26" cy="28" r="3" fill="#333"/>
              <circle cx="38" cy="28" r="3" fill="#333"/>
              <circle cx="27" cy="27" r="1" fill="#fff"/>
              <circle cx="39" cy="27" r="1" fill="#fff"/>
              <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
              <rect x="40" y="30" width="10" height="10" rx="2" fill="#FF6347"/>
              <rect x="42" y="32" width="6" height="6" rx="1" fill="#FFA07A"/>
            </svg>
          </view>
          <view class="food-info">
            <text class="food-name">{{ item.foodName }}</text>
            <text class="food-calorie">{{ item.calories }} kcal</text>
          </view>
          <view class="food-actions">
            <view class="delete-btn" @tap.stop="handleDelete(item)">
              <text class="delete-icon">×</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty-state">
        <svg viewBox="0 0 100 100" class="empty-cat-icon">
          <ellipse cx="50" cy="70" rx="25" ry="15" fill="#FFA07A"/>
          <circle cx="50" cy="45" r="22" fill="#FFA07A"/>
          <ellipse cx="35" cy="35" rx="8" ry="12" fill="#FFA07A"/>
          <ellipse cx="65" cy="35" rx="8" ry="12" fill="#FFA07A"/>
          <ellipse cx="35" cy="37" rx="5" ry="8" fill="#FFB88A"/>
          <ellipse cx="65" cy="37" rx="5" ry="8" fill="#FFB88A"/>
          <circle cx="42" cy="42" r="3" fill="#333"/>
          <circle cx="58" cy="42" r="3" fill="#333"/>
          <circle cx="43" cy="41" r="1" fill="#fff"/>
          <circle cx="59" cy="41" r="1" fill="#fff"/>
          <ellipse cx="50" cy="52" rx="3" ry="2" fill="#FF6347"/>
          <path d="M46 56 Q50 60 54 56" stroke="#333" stroke-width="1.5" fill="none"/>
          <line x1="28" y1="50" x2="15" y2="48" stroke="#333" stroke-width="1"/>
          <line x1="28" y1="52" x2="15" y2="53" stroke="#333" stroke-width="1"/>
          <line x1="72" y1="50" x2="85" y2="48" stroke="#333" stroke-width="1"/>
          <line x1="72" y1="52" x2="85" y2="53" stroke="#333" stroke-width="1"/>
        </svg>
        <text class="empty-text">暂无午餐记录</text>
      </view>
    </scroll-view>

    <view class="bottom-btn-wrap">
      <view class="add-btn" @tap="handleAdd">
        <svg viewBox="0 0 48 48" class="add-icon">
          <circle cx="24" cy="24" r="20" fill="#FF6347"/>
          <circle cx="18" cy="22" r="3" fill="#333"/>
          <circle cx="30" cy="22" r="3" fill="#333"/>
          <circle cx="19" cy="21" r="1" fill="#fff"/>
          <circle cx="31" cy="21" r="1" fill="#fff"/>
          <path d="M24 28 Q22 31 24 34 Q26 31 24 28" stroke="#333" stroke-width="1.5" fill="none"/>
          <rect x="16" y="36" width="4" height="8" rx="2" fill="#333"/>
          <rect x="28" y="36" width="4" height="8" rx="2" fill="#333"/>
          <rect x="12" y="40" width="24" height="4" rx="2" fill="#333"/>
        </svg>
        <text class="add-text">添加午餐</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getTodayRecords } from '@/api/history/history'
import { deleteDietRecord } from '@/api/add/add'
import type { DietItemVO, DailyDietVO, DietRecordVO } from '@/api/types'
import { MealType } from '@/api/types'

const MEAL_TYPE = MealType.LUNCH
const foodList = ref<DietItemVO[]>([])

const today = new Date()
const selectedDate = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`)

const totalCalories = computed(() => {
  return foodList.value.reduce((sum, item) => sum + Number(item.calories || 0), 0)
})

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const weekday = weekdays[date.getDay()]
  return `${month}月${day}日 ${weekday}`
}

function getDateLabel() {
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  if (selectedDate.value === todayStr) {
    return '今日'
  }
  return ''
}

function handleDateChange(e: { detail: { value: string } }) {
  selectedDate.value = e.detail.value
  loadData()
}

async function loadData() {
  try {
    const response = await getTodayRecords(selectedDate.value)
    if (response && response.data) {
      const dailyDiet = response.data as DailyDietVO
      if (dailyDiet.records && dailyDiet.records.length > 0) {
        const mealRecords = dailyDiet.records.filter((r: DietRecordVO) => 
          String(r.mealType) === String(MEAL_TYPE)
        )
        const allItems: DietItemVO[] = []
        mealRecords.forEach(record => {
          if (record.items && record.items.length > 0) {
            allItems.push(...record.items)
          }
        })
        foodList.value = allItems
      } else {
        foodList.value = []
      }
    } else {
      foodList.value = []
    }
  } catch (e) {
    console.error('Failed to load diet records:', e)
    foodList.value = []
  }
}

function handleAdd() {
  uni.navigateTo({ url: `/pages/add/lunch/edit?mode=add&date=${selectedDate.value}` })
}

function handleEdit(item: DietItemVO) {
  uni.navigateTo({ url: `/pages/add/lunch/edit?mode=edit&itemId=${item.id}&date=${selectedDate.value}` })
}

async function handleDelete(item: DietItemVO) {
  uni.showModal({
    title: '删除确认',
    content: `确定要删除"${item.foodName}"吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteDietRecord(String(item.recordId))
          uni.showToast({ title: '删除成功', icon: 'success' })
          await loadData()
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onMounted(() => {
  loadData()
})

onShow(() => {
  loadData()
})

uni.$on('dietUpdated', () => {
  loadData()
})
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFFBF5 0%, #FFF8F0 100%);
  padding-bottom: 180rpx;
  position: relative;
}

.page-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='2' fill='%23FFA07A' opacity='0.15'/%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.page-container > * {
  position: relative;
  z-index: 1;
}

.header-card {
  background: linear-gradient(135deg, #FFA07A 0%, #FF6347 100%);
  border-radius: 40rpx;
  padding: 40rpx;
  margin: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(255, 99, 71, 0.25);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
}

.header-title {
  font-size: 40rpx;
  font-weight: 600;
  color: #FFFFFF;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.checkin-icon-wrap {
  width: 64rpx;
  height: 64rpx;
}

.checkin-icon {
  width: 100%;
  height: 100%;
}

.date-picker-wrap {
  display: flex;
  justify-content: center;
  margin: 16rpx 0;
}

.date-picker-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 32rpx;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 40rpx;
  backdrop-filter: blur(10px);
}

.date-icon {
  font-size: 28rpx;
}

.date-text {
  font-size: 28rpx;
  color: #FFFFFF;
  font-weight: 500;
}

.date-arrow {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.8);
}

.calorie-summary {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.summary-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.summary-value {
  font-size: 56rpx;
  font-weight: 700;
  color: #FFFFFF;
  text-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.15);
}

.summary-unit {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}

.food-scroll {
  height: calc(100vh - 400rpx);
  margin: 0 24rpx;
}

.food-list {
  padding-right: 8rpx;
}

.food-card {
  display: flex;
  align-items: center;
  padding: 28rpx;
  margin-bottom: 20rpx;
  background: #FFFFFF;
  border-radius: 32rpx;
  box-shadow: 0 8rpx 20rpx rgba(255, 160, 122, 0.12);
  border: 2rpx solid rgba(255, 160, 122, 0.2);
}

.food-image {
  width: 120rpx;
  height: 120rpx;
  border-radius: 24rpx;
  background: rgba(255, 160, 122, 0.1);
  margin-right: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.food-photo {
  width: 100%;
  height: 100%;
}

.food-icon {
  width: 88rpx;
  height: 88rpx;
}

.food-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.food-name {
  font-size: 32rpx;
  color: #555555;
  font-weight: 500;
}

.food-detail {
  font-size: 24rpx;
  color: #999999;
}

.food-remark {
  font-size: 24rpx;
  color: #FF6347;
}

.food-calorie {
  font-size: 28rpx;
  color: #FF6347;
  font-weight: 500;
}

.food-actions {
  display: flex;
  align-items: center;
}

.delete-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(255, 99, 71, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-icon {
  font-size: 36rpx;
  color: #FF6347;
  line-height: 1;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-cat-icon {
  width: 200rpx;
  height: 200rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #B8B8B8;
  margin-top: 32rpx;
}

.bottom-btn-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: rgba(255, 251, 245, 0.95);
  backdrop-filter: blur(20rpx);
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, #FF6347 0%, #FFA07A 100%);
  border-radius: 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 99, 71, 0.3);
}

.add-icon {
  width: 48rpx;
  height: 48rpx;
}

.add-text {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 600;
}
</style>
