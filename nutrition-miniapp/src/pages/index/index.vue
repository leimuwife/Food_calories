<template>
  <view class="page-container">
    <view class="calorie-card">
      <view class="card-header">
        <text class="card-title">食光笔记</text>
        <view class="checkin-btn" @tap="handleCheckin">
          <view class="checkin-icon-wrap">
            <svg viewBox="0 0 64 64" class="icon">
              <circle cx="32" cy="32" r="28" fill="#FFB6C1"/>
              <circle cx="24" cy="28" r="4" fill="#333"/>
              <circle cx="40" cy="28" r="4" fill="#333"/>
              <circle cx="25" cy="27" r="1.5" fill="#fff"/>
              <circle cx="41" cy="27" r="1.5" fill="#fff"/>
              <path d="M32 36 Q30 40 32 44 Q34 40 32 36" stroke="#333" stroke-width="2" fill="none"/>
              <rect x="38" y="18" width="14" height="12" rx="2" fill="#FF69B4"/>
              <text x="45" y="27" font-size="8" fill="#fff" text-anchor="middle">OK</text>
            </svg>
          </view>
          <text class="checkin-text">减肥打卡</text>
        </view>
      </view>
      <view class="calorie-display">
        <text class="calorie-value">{{ todayCalories }}</text>
        <text class="calorie-unit">kcal</text>
      </view>
    </view>

    <view class="search-entry-card">
      <view class="search-row">
        <view class="search-input-wrap">
          <svg viewBox="0 0 48 48" class="cat-paw-icon">
            <circle cx="24" cy="30" r="8" fill="#FFB6C1"/>
            <circle cx="14" cy="22" r="5" fill="#FFB6C1"/>
            <circle cx="34" cy="22" r="5" fill="#FFB6C1"/>
            <circle cx="24" cy="16" r="5" fill="#FFB6C1"/>
            <circle cx="18" cy="30" r="3" fill="#FF69B4"/>
            <circle cx="30" cy="30" r="3" fill="#FF69B4"/>
            <circle cx="24" cy="22" r="3" fill="#FF69B4"/>
            <circle cx="16" cy="24" r="2" fill="#FF69B4"/>
            <circle cx="32" cy="24" r="2" fill="#FF69B4"/>
          </svg>
          <text class="search-placeholder">搜索食物</text>
        </view>
        <view class="history-btn" @tap="handleHistory">
          <svg viewBox="0 0 48 48" class="cat-lay-icon">
            <ellipse cx="24" cy="36" rx="18" ry="8" fill="#FFB6C1"/>
            <circle cx="18" cy="22" r="10" fill="#FFB6C1"/>
            <circle cx="30" cy="22" r="10" fill="#FFB6C1"/>
            <circle cx="24" cy="32" r="6" fill="#FFB6C1"/>
            <circle cx="16" cy="20" r="2" fill="#333"/>
            <circle cx="32" cy="20" r="2" fill="#333"/>
            <path d="M24 26 Q22 30 24 32 Q26 30 24 26" stroke="#333" stroke-width="1.5" fill="none"/>
            <ellipse cx="12" cy="26" rx="3" ry="4" fill="#FFC0CB"/>
            <ellipse cx="36" cy="26" rx="3" ry="4" fill="#FFC0CB"/>
          </svg>
          <text class="history-text">历史记录</text>
        </view>
      </view>
      <view class="quick-entry">
        <view v-for="item in quickItems" :key="item.key" class="entry-item" @tap="handleQuickTap(item.key)">
          <view class="entry-icon-wrap">
            <svg viewBox="0 0 64 64" class="entry-icon">
              <template v-if="item.key === 'breakfast'">
                <circle cx="32" cy="32" r="24" fill="#FFB347"/>
                <circle cx="26" cy="28" r="3" fill="#333"/>
                <circle cx="38" cy="28" r="3" fill="#333"/>
                <circle cx="27" cy="27" r="1" fill="#fff"/>
                <circle cx="39" cy="27" r="1" fill="#fff"/>
                <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
                <rect x="38" y="30" width="14" height="10" rx="2" fill="#8B4513"/>
                <rect x="38" y="30" width="14" height="4" rx="1" fill="#FFD700"/>
              </template>
              <template v-else-if="item.key === 'lunch'">
                <circle cx="32" cy="32" r="24" fill="#F5F5F5"/>
                <circle cx="26" cy="28" r="3" fill="#333"/>
                <circle cx="38" cy="28" r="3" fill="#333"/>
                <circle cx="27" cy="27" r="1" fill="#fff"/>
                <circle cx="39" cy="27" r="1" fill="#fff"/>
                <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
                <ellipse cx="42" cy="36" rx="8" ry="6" fill="#8B4513"/>
                <circle cx="40" cy="34" r="2" fill="#FFD700"/>
                <circle cx="44" cy="35" r="1.5" fill="#FFD700"/>
                <circle cx="42" cy="37" r="1.5" fill="#FFD700"/>
              </template>
              <template v-else-if="item.key === 'dinner'">
                <circle cx="32" cy="32" r="24" fill="#808080"/>
                <circle cx="26" cy="28" r="3" fill="#333"/>
                <circle cx="38" cy="28" r="3" fill="#333"/>
                <circle cx="27" cy="27" r="1" fill="#fff"/>
                <circle cx="39" cy="27" r="1" fill="#fff"/>
                <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
                <ellipse cx="32" cy="48" rx="14" ry="4" fill="#E8E8E8"/>
                <path d="M22 48 L42 48" stroke="#D0D0D0" stroke-width="1"/>
              </template>
              <template v-else-if="item.key === 'snack'">
                <circle cx="32" cy="32" r="14" fill="#FFB6C1"/>
                <circle cx="46" cy="32" r="14" fill="#87CEEB"/>
                <circle cx="32" cy="46" r="14" fill="#FFD700"/>
                <circle cx="24" cy="24" r="2" fill="#333"/>
                <circle cx="36" cy="24" r="2" fill="#333"/>
                <circle cx="44" cy="24" r="2" fill="#333"/>
                <circle cx="56" cy="24" r="2" fill="#333"/>
                <circle cx="24" cy="44" r="2" fill="#333"/>
                <circle cx="36" cy="44" r="2" fill="#333"/>
                <path d="M32 30 Q30 33 32 35 Q34 33 32 30" stroke="#333" stroke-width="1" fill="none"/>
                <path d="M46 30 Q44 33 46 35 Q48 33 46 30" stroke="#333" stroke-width="1" fill="none"/>
                <path d="M32 44 Q30 47 32 49 Q34 47 32 44" stroke="#333" stroke-width="1" fill="none"/>
              </template>
              <template v-else-if="item.key === 'nutritionist'">
                <circle cx="32" cy="32" r="24" fill="#FFB6C1"/>
                <circle cx="26" cy="28" r="3" fill="#333"/>
                <circle cx="38" cy="28" r="3" fill="#333"/>
                <circle cx="27" cy="27" r="1" fill="#fff"/>
                <circle cx="39" cy="27" r="1" fill="#fff"/>
                <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
                <path d="M18 20 L32 8 L46 20" fill="#fff"/>
                <circle cx="32" cy="8" r="4" fill="#FF69B4"/>
              </template>
            </svg>
          </view>
          <text class="entry-name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <view class="food-list-card">
      <view class="card-header">
        <text class="card-title">今日记录</text>
      </view>
      <scroll-view scroll-y class="food-scroll">
        <view v-if="foodList.length > 0" class="food-list">
          <view v-for="item in foodList" :key="item.id" class="food-item">
            <view class="food-image">
              <svg viewBox="0 0 64 64" class="food-icon">
                <circle cx="32" cy="32" r="24" fill="#FFB6C1"/>
                <circle cx="26" cy="28" r="3" fill="#333"/>
                <circle cx="38" cy="28" r="3" fill="#333"/>
                <circle cx="27" cy="27" r="1" fill="#fff"/>
                <circle cx="39" cy="27" r="1" fill="#fff"/>
                <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
                <rect x="40" y="30" width="10" height="10" rx="2" fill="#FF69B4"/>
                <rect x="42" y="32" width="6" height="6" rx="1" fill="#FFB6C1"/>
              </svg>
            </view>
            <view class="food-info">
              <text class="food-name">{{ item.name }}</text>
              <view class="food-tag" :class="item.mealType">{{ item.mealTypeText }}</view>
            </view>
            <text class="food-calorie">{{ item.calories }}kcal</text>
          </view>
        </view>
        <view v-else class="empty-state">
          <svg viewBox="0 0 100 100" class="empty-cat-icon">
            <ellipse cx="50" cy="70" rx="25" ry="15" fill="#FFB6C1"/>
            <circle cx="50" cy="45" r="22" fill="#FFB6C1"/>
            <ellipse cx="35" cy="35" rx="8" ry="12" fill="#FFB6C1"/>
            <ellipse cx="65" cy="35" rx="8" ry="12" fill="#FFB6C1"/>
            <ellipse cx="35" cy="37" rx="5" ry="8" fill="#FFC0CB"/>
            <ellipse cx="65" cy="37" rx="5" ry="8" fill="#FFC0CB"/>
            <circle cx="42" cy="42" r="3" fill="#333"/>
            <circle cx="58" cy="42" r="3" fill="#333"/>
            <circle cx="43" cy="41" r="1" fill="#fff"/>
            <circle cx="59" cy="41" r="1" fill="#fff"/>
            <ellipse cx="50" cy="52" rx="3" ry="2" fill="#FF69B4"/>
            <path d="M46 56 Q50 60 54 56" stroke="#333" stroke-width="1.5" fill="none"/>
            <line x1="28" y1="50" x2="15" y2="48" stroke="#333" stroke-width="1"/>
            <line x1="28" y1="52" x2="15" y2="53" stroke="#333" stroke-width="1"/>
            <line x1="72" y1="50" x2="85" y2="48" stroke="#333" stroke-width="1"/>
            <line x1="72" y1="52" x2="85" y2="53" stroke="#333" stroke-width="1"/>
          </svg>
          <text class="empty-text">今日暂无饮食记录</text>
        </view>
      </scroll-view>
    </view>

    <view class="tab-bar">
      <view v-for="tab in tabItems" :key="tab.key" :class="['tab-item', { 'tab-item-active': activeTab === tab.key }]" @tap="handleTabTap(tab.key)">
        <svg viewBox="0 0 64 64" class="tab-icon">
          <template v-if="tab.key === 'home'">
            <path d="M8 40 L8 20 L32 8 L56 20 L56 40 Z" fill="#FFB6C1"/>
            <path d="M8 40 L32 28 L56 40" stroke="#FF69B4" stroke-width="2" fill="none"/>
            <circle cx="32" cy="18" r="5" fill="#FFB6C1"/>
            <circle cx="32" cy="16" r="2" fill="#333"/>
            <circle cx="30" cy="15" r="0.8" fill="#fff"/>
          </template>
          
          <template v-else-if="tab.key === 'circle'">
            <circle cx="32" cy="32" r="20" fill="#FFB6C1"/>
            <circle cx="26" cy="28" r="3" fill="#333"/>
            <circle cx="38" cy="28" r="3" fill="#333"/>
            <circle cx="27" cy="27" r="1" fill="#fff"/>
            <circle cx="39" cy="27" r="1" fill="#fff"/>
            <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
            <circle cx="48" cy="22" r="10" fill="#FF69B4"/>
            <path d="M48 14 L48 8 M54 20 L60 20 M42 20 L36 20 M48 30 L48 36" stroke="#FFB6C1" stroke-width="2"/>
            <circle cx="48" cy="14" r="3" fill="#FFB6C1"/>
            <circle cx="60" cy="20" r="3" fill="#FFB6C1"/>
            <circle cx="36" cy="20" r="3" fill="#FFB6C1"/>
            <circle cx="48" cy="36" r="3" fill="#FFB6C1"/>
          </template>
          <template v-else-if="tab.key === 'profile'">
            <circle cx="32" cy="32" r="22" fill="#FFB6C1"/>
            <circle cx="26" cy="28" r="3" fill="#333"/>
            <circle cx="38" cy="28" r="3" fill="#333"/>
            <circle cx="27" cy="27" r="1" fill="#fff"/>
            <circle cx="39" cy="27" r="1" fill="#fff"/>
            <path d="M32 35 Q30 38 32 41 Q34 38 32 35" stroke="#333" stroke-width="1.5" fill="none"/>
            <path d="M22 18 Q32 8 42 18" fill="#FF69B4"/>
            <path d="M30 14 Q32 8 34 14" fill="#FFB6C1"/>
          </template>
        </svg>
        <text class="tab-text">{{ tab.name }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getTodayRecords, getDailySummary, searchFood } from '@/api/shouye'
import { getToday } from '@/utils'

const userStore = useUserStore()

interface FoodListItem {
  id: number
  name: string
  mealType: string
  mealTypeText: string
  calories: number
}

const todayCalories = ref(0)
const activeTab = ref('home')
const foodList = ref<FoodListItem[]>([])

const quickItems = [
  { key: 'breakfast', name: '早餐' },
  { key: 'lunch', name: '午餐' },
  { key: 'dinner', name: '晚餐' },
  { key: 'snack', name: '夜宵' },
  { key: 'nutritionist', name: '小张营养师' },
]

const tabItems = [
  { key: 'home', name: '首页' },
  { key: 'circle', name: '轻友圈' },
  { key: 'profile', name: '我的' },
]

const mealTypeMap: Record<string, string> = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐',
  snack: '加餐',
}

async function loadTodayData() {
  const today = getToday()
  try {
    const [recordsRes, summaryRes] = await Promise.all([
      getTodayRecords(today),
      getDailySummary(today),
    ])

    todayCalories.value = summaryRes.data.totalCalories || 0

    const records = recordsRes.data.records || []
    const items: FoodListItem[] = []

    records.forEach((record) => {
      const mealTypeText = mealTypeMap[record.mealType] || record.mealType
      record.items.forEach((item) => {
        items.push({
          id: item.id,
          name: item.foodName,
          mealType: record.mealType,
          mealTypeText,
          calories: item.calories,
        })
      })
    })

    foodList.value = items
  } catch (e) {
    console.error('加载今日数据失败:', e)
  }
}

async function handleSearch(keyword: string) {
  if (!keyword.trim()) return
  try {
    const res = await searchFood(keyword)
    console.log('搜索结果:', res.data.list)
  } catch (e) {
    console.error('搜索失败:', e)
  }
}

function handleCheckin() {
  uni.navigateTo({ url: '/pages/daka/index' })
}

function handleHistory() {
  console.log('History tap')
}

function handleQuickTap(key: string) {
  console.log('Quick entry tap:', key)
  if (key === 'breakfast') {
    uni.navigateTo({ url: '/pages/add/breakFast/index' })
  } else if (key === 'lunch') {
    uni.navigateTo({ url: '/pages/add/lunch/index' })
  } else if (key === 'dinner') {
    uni.navigateTo({ url: '/pages/add/dinner/index' })
  } else if (key === 'snack') {
    uni.navigateTo({ url: '/pages/add/snack/index' })
  } else if (key === 'nutritionist') {
    uni.navigateTo({ url: '/pages/yingyangshi/index' })
  }
}

function handleTabTap(key: string) {
  activeTab.value = key
  if (key === 'circle') {
    uni.navigateTo({ url: '/pages/qingyouquan/index' })
  } else if (key === 'profile') {
    uni.navigateTo({ url: '/pages/wode/index' })
  }
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/weChatLogin/index' })
    return
  }
  loadTodayData()
})
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #FFF9FA 0%, #FFF5F7 100%);
  padding: 24rpx;
  padding-bottom: 140rpx;
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

.calorie-card {
  background: linear-gradient(135deg, #FFB6C1 0%, #FF69B4 100%);
  border-radius: 40rpx;
  padding: 40rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 12rpx 32rpx rgba(255, 105, 180, 0.25);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.card-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #FFFFFF;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
}

.checkin-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.checkin-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  border: 3rpx solid rgba(255, 182, 193, 0.6);
  padding: 6rpx;
  background: rgba(255, 255, 255, 0.3);
}

.checkin-icon-wrap .icon {
  width: 100%;
  height: 100%;
}

.checkin-text {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.calorie-display {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
}

.calorie-value {
  font-size: 80rpx;
  font-weight: 700;
  color: #FFFFFF;
  line-height: 1;
  text-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.15);
}

.calorie-unit {
  font-size: 32rpx;
  color: rgba(255, 255, 255, 0.9);
}

.search-entry-card {
  background: #FFFFFF;
  border-radius: 40rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 105, 180, 0.1);
  border: 2rpx solid rgba(255, 182, 193, 0.3);
}

.search-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 32rpx;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #FFF9FA;
  border-radius: 48rpx;
  padding: 24rpx 32rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.4);
}

.cat-paw-icon {
  width: 40rpx;
  height: 40rpx;
}

.search-placeholder {
  font-size: 28rpx;
  color: #B8B8B8;
  margin-left: 20rpx;
}

.history-btn {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 28rpx;
  background: rgba(255, 182, 193, 0.15);
  border-radius: 32rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.3);
}

.cat-lay-icon {
  width: 40rpx;
  height: 40rpx;
}

.history-text {
  font-size: 26rpx;
  color: #FF69B4;
}

.quick-entry {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
}

.entry-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.entry-icon-wrap {
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  background: linear-gradient(145deg, #FFF0F3 0%, #FFE4E9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(255, 105, 180, 0.12);
  border: 2rpx solid rgba(255, 182, 193, 0.2);
}

.entry-icon {
  width: 72rpx;
  height: 72rpx;
}

.entry-name {
  font-size: 24rpx;
  color: #FF69B4;
  font-weight: 500;
}

.food-list-card {
  background: #FFFFFF;
  border-radius: 40rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 105, 180, 0.1);
  border: 2rpx solid rgba(255, 182, 193, 0.3);
}

.food-scroll {
  height: 480rpx;
  margin-top: 16rpx;
}

.food-list {
  padding-right: 8rpx;
}

.food-item {
  display: flex;
  align-items: center;
  padding: 24rpx 20rpx;
  margin-bottom: 16rpx;
  background: #FFF9FA;
  border-radius: 28rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.2);
}

.food-item:last-child {
  margin-bottom: 0;
}

.food-image {
  width: 100rpx;
  height: 100rpx;
  border-radius: 20rpx;
  background: rgba(255, 182, 193, 0.1);
  margin-right: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.food-icon {
  width: 72rpx;
  height: 72rpx;
}

.food-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.food-name {
  font-size: 30rpx;
  color: #555555;
  font-weight: 500;
}

.food-tag {
  display: inline-block;
  font-size: 22rpx;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  align-self: flex-start;
}

.food-tag.breakfast {
  background: rgba(255, 179, 71, 0.15);
  color: #FFA500;
}

.food-tag.lunch {
  background: rgba(255, 105, 180, 0.15);
  color: #FF69B4;
}

.food-tag.dinner {
  background: rgba(128, 128, 128, 0.15);
  color: #666666;
}

.food-tag.snack {
  background: rgba(255, 215, 0, 0.15);
  color: #FFD700;
}

.food-calorie {
  font-size: 30rpx;
  color: #FF69B4;
  font-weight: 600;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
}

.empty-cat-icon {
  width: 160rpx;
  height: 160rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #B8B8B8;
  margin-top: 24rpx;
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  display: flex;
  justify-content: space-around;
  padding: 24rpx 0 48rpx;
  box-shadow: 0 -8rpx 32rpx rgba(255, 105, 180, 0.12);
  border-top-left-radius: 40rpx;
  border-top-right-radius: 40rpx;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.tab-icon {
  width: 56rpx;
  height: 56rpx;
  opacity: 0.5;
  transition: all 0.3s ease;
}

.tab-item-active .tab-icon {
  opacity: 1;
}

.tab-text {
  font-size: 24rpx;
  color: #B8B8B8;
  transition: all 0.3s ease;
}

.tab-item-active .tab-text {
  color: #FF69B4;
  font-weight: 500;
}
</style>
