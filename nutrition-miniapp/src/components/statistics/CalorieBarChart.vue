<template>
  <view class="chart-card">
    <view class="chart-header">
      <text class="chart-title">{{ title }}</text>
      <text class="chart-unit">kcal / 日</text>
    </view>

    <view v-if="normalizedData.length === 0" class="chart-empty">暂无热量数据</view>
    <scroll-view v-else scroll-x class="chart-scroll" :show-scrollbar="false">
      <view class="bars" :style="barsStyle">
        <view v-for="item in normalizedData" :key="item.date" class="bar-column">
          <text class="bar-value">{{ formatCalories(item.calories) }}</text>
          <view class="bar-area">
            <view
              :class="['bar', { 'bar-empty': item.calories <= 0 }]"
              :style="{ height: getBarHeight(item.calories) }"
            ></view>
          </view>
          <text class="bar-date">{{ formatDate(item.date) }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DailyCaloriePoint } from '@/api/types'

const props = defineProps<{
  title: string
  data: DailyCaloriePoint[]
  scrollable?: boolean
}>()

const normalizedData = computed(() => (props.data || []).map(item => ({
  date: item.date,
  calories: Number(item.calories) || 0,
})))

const maxCalories = computed(() => Math.max(...normalizedData.value.map(item => item.calories), 1))

const barsStyle = computed(() => ({
  minWidth: props.scrollable ? `${Math.max(normalizedData.value.length * 56, 700)}rpx` : '100%',
}))

function getBarHeight(calories: number): string {
  if (calories <= 0) return '4rpx'
  return `${Math.max((calories / maxCalories.value) * 100, 6)}%`
}

function formatCalories(calories: number): string {
  return String(Math.round(calories))
}

function formatDate(date: string): string {
  const parts = date.split('-')
  return parts.length >= 3 ? `${parts[1]}-${parts[2]}` : date
}
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;

.chart-card {
  flex: 1 1 520rpx;
  min-width: 0;
  padding: 28rpx 24rpx 22rpx;
  box-sizing: border-box;
  background: #FFFFFF;
  border-radius: 32rpx;
  box-shadow: 0 10rpx 30rpx rgba(255, 105, 180, 0.1);
  border: 1rpx solid rgba(255, 182, 193, 0.28);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.chart-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #4B3340;
}

.chart-unit {
  font-size: 21rpx;
  color: #B895A3;
}

.chart-empty {
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #B895A3;
  font-size: 25rpx;
}

.chart-scroll {
  width: 100%;
}

.bars {
  width: 100%;
  min-width: 100%;
  height: 330rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  gap: 8rpx;
  padding-top: 10rpx;
  box-sizing: border-box;
}

.bar-column {
  width: 48rpx;
  flex: 1 0 48rpx;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}

.bar-value {
  height: 28rpx;
  font-size: 18rpx;
  color: #8C6877;
  white-space: nowrap;
}

.bar-area {
  width: 30rpx;
  height: 230rpx;
  margin: 6rpx 0 10rpx;
  border-radius: 14rpx 14rpx 6rpx 6rpx;
  background: linear-gradient(180deg, #FFF1F6 0%, #FFE4EC 100%);
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar {
  width: 100%;
  min-height: 4rpx;
  border-radius: 14rpx 14rpx 6rpx 6rpx;
  background: linear-gradient(180deg, #FF8FC3 0%, $primary-color 100%);
  transition: height 0.25s ease;
}

.bar-empty {
  background: #F3D8E2;
}

.bar-date {
  font-size: 18rpx;
  color: #A58A95;
  white-space: nowrap;
}
</style>
