<template>
  <view class="page-container">
    <!-- 顶部绿色卡片 - 今日饮食热量 -->
    <view class="calorie-card">
      <view class="card-header">
        <text class="card-title">今日饮食</text>
        <image class="checkin-icon" src="/static/images/shouye/daka.png" mode="aspectFit" @tap="handleCheckin" />
      </view>
      <view class="calorie-display">
        <text class="calorie-value">{{ todayCalories }}</text>
        <text class="calorie-unit">kcal</text>
      </view>
    </view>

    <!-- 搜索与快捷入口卡片 -->
    <view class="search-entry-card">
      <view class="search-row">
        <view class="search-input-wrap">
          <u-icon name="search" size="20" color="#7EC8A0" />
          <text class="search-placeholder">搜索食物</text>
        </view>
        <view class="history-btn" @tap="handleHistory">
          <u-icon name="clock" size="18" color="#7EC8A0" />
          <text class="history-text">历史记录</text>
        </view>
      </view>
      <view class="quick-entry">
        <view v-for="item in quickItems" :key="item.key" class="entry-item" @tap="handleQuickTap(item.key)">
          <view class="entry-icon-wrap">
            <u-icon :name="item.icon" size="24" color="#5DB88B" />
          </view>
          <text class="entry-name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <!-- 今日食物列表卡片 -->
    <view class="food-list-card">
      <view class="card-header">
        <text class="card-title">今日记录</text>
      </view>
      <scroll-view scroll-y class="food-scroll">
        <view v-if="foodList.length > 0" class="food-list">
          <view v-for="item in foodList" :key="item.id" class="food-item">
            <view class="food-image" />
            <view class="food-info">
              <text class="food-name">{{ item.name }}</text>
              <view class="food-tag" :class="item.mealType">{{ item.mealTypeText }}</view>
            </view>
            <text class="food-calorie">{{ item.calories }}kcal</text>
          </view>
        </view>
        <view v-else class="empty-state">
          <u-icon name="empty-page" size="40" color="#DCDFE6" />
          <text class="empty-text">今日暂无饮食记录</text>
        </view>
      </scroll-view>
    </view>

    <!-- 底部固定Tab导航栏 -->
    <view class="tab-bar">
      <view v-for="tab in tabItems" :key="tab.key" :class="['tab-item', { 'tab-item-active': activeTab === tab.key }]" @tap="handleTabTap(tab.key)">
        <u-icon :name="tab.icon" :size="26" :color="activeTab === tab.key ? '#7EC8A0' : '#909399'" />
        <text class="tab-text">{{ tab.name }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTodayRecords, getDailySummary, searchFood } from '@/api/shouye'
import { getToday } from '@/utils'

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
  { key: 'breakfast', name: '早餐', icon: 'calendar' },
  { key: 'lunch', name: '午餐', icon: 'clock-fill' },
  { key: 'dinner', name: '晚餐', icon: 'clock' },
  { key: 'snack', name: '夜宵', icon: 'hourglass' },
  { key: 'nutritionist', name: '小张营养师', icon: 'man' },
]

const tabItems = [
  { key: 'home', name: '首页', icon: 'home' },
  { key: 'message', name: '私信', icon: 'chat' },
  { key: 'circle', name: '轻友圈', icon: 'moments' },
  { key: 'profile', name: '我的', icon: 'account' },
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
  console.log('Checkin tap')
}

function handleHistory() {
  console.log('History tap')
}

function handleQuickTap(key: string) {
  console.log('Quick entry tap:', key)
}

function handleTabTap(key: string) {
  activeTab.value = key
  console.log('Tab tap:', key)
}

onMounted(() => {
  loadTodayData()
})
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #F5F7FA;
  padding: 24rpx;
  padding-bottom: 140rpx;
}

.calorie-card {
  background: linear-gradient(135deg, #7EC8A0 0%, #5DB88B 100%);
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(126, 200, 160, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.checkin-icon {
  width: 48rpx;
  height: 48rpx;
}

.calorie-display {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.calorie-value {
  font-size: 72rpx;
  font-weight: 700;
  color: #FFFFFF;
  line-height: 1;
}

.calorie-unit {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}

.search-entry-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.search-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #F5F7FA;
  border-radius: 40rpx;
  padding: 20rpx 24rpx;
}

.search-placeholder {
  font-size: 28rpx;
  color: #909399;
  margin-left: 16rpx;
}

.history-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 20rpx;
  background: #F5F7FA;
  border-radius: 24rpx;
}

.history-text {
  font-size: 26rpx;
  color: #606266;
}

.quick-entry {
  display: flex;
  justify-content: space-between;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.entry-icon-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: #E8F4ED;
  display: flex;
  align-items: center;
  justify-content: center;
}

.entry-name {
  font-size: 24rpx;
  color: #303133;
}

.food-list-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.food-scroll {
  height: 400rpx;
  margin-top: 8rpx;
}

.food-list {
  padding-right: 8rpx;
}

.food-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F5F7FA;
}

.food-item:last-child {
  border-bottom: none;
}

.food-image {
  width: 88rpx;
  height: 88rpx;
  border-radius: 16rpx;
  background: #F5F7FA;
  margin-right: 20rpx;
}

.food-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.food-name {
  font-size: 30rpx;
  color: #303133;
  font-weight: 500;
}

.food-tag {
  display: inline-block;
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 10rpx;
  align-self: flex-start;
}

.food-tag.breakfast {
  background: #FFF4E6;
  color: #E6A23C;
}

.food-tag.lunch {
  background: #E8F4ED;
  color: #67C23A;
}

.food-tag.dinner {
  background: #E6F7FF;
  color: #409EFF;
}

.food-tag.snack {
  background: #FFF0F6;
  color: #E85D75;
}

.food-calorie {
  font-size: 28rpx;
  color: #7EC8A0;
  font-weight: 600;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
}

.empty-text {
  font-size: 26rpx;
  color: #909399;
  margin-top: 16rpx;
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #FFFFFF;
  display: flex;
  justify-content: space-around;
  padding: 20rpx 0 44rpx;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.tab-text {
  font-size: 24rpx;
  color: #909399;
}

.tab-item-active .tab-text {
  color: #7EC8A0;
  font-weight: 500;
}
</style>
