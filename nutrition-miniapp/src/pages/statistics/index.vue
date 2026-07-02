<template>
  <view class="page-container">
    <!-- 日/月切换 -->
    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ 'tab-active': activeTab === tab.value }"
        @tap="activeTab = tab.value"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- 日期/月份选择器 -->
    <view class="date-selector" @tap="showPicker = true">
      <u-icon name="calendar" size="18" color="#7EC8A0" />
      <text class="selector-text">{{ activeTab === 'daily' ? currentDateStr : currentMonthStr }}</text>
      <u-icon name="arrow-down" size="14" color="#909399" />
    </view>

    <!-- ========== 日统计 ========== -->
    <view v-if="activeTab === 'daily' && dailyData" class="stats-content">
      <!-- 供能占比卡片 -->
      <view class="card">
        <text class="card-title">三大营养素供能占比</text>
        <view class="ratio-chart">
          <view class="ratio-bar">
            <view class="ratio-segment protein-seg" :style="{ width: dailyData.proteinRatio + '%' }" />
            <view class="ratio-segment fat-seg" :style="{ width: dailyData.fatRatio + '%' }" />
            <view class="ratio-segment carbs-seg" :style="{ width: dailyData.carbsRatio + '%' }" />
          </view>
          <view class="ratio-legend">
            <view class="legend-item">
              <view class="legend-dot protein-dot" /><text>蛋白质 {{ dailyData.proteinRatio }}%</text>
            </view>
            <view class="legend-item">
              <view class="legend-dot fat-dot" /><text>脂肪 {{ dailyData.fatRatio }}%</text>
            </view>
            <view class="legend-item">
              <view class="legend-dot carbs-dot" /><text>碳水 {{ dailyData.carbsRatio }}%</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 各餐次占比 -->
      <view class="card" v-if="dailyData.meals">
        <text class="card-title">各餐次热量占比</text>
        <view class="meal-bars">
          <view
            v-for="(meal, type) in dailyData.meals"
            :key="type"
            class="meal-bar-row"
          >
            <text class="meal-bar-label">{{ mealTypeMap[type as string] || type }}</text>
            <view class="meal-bar-track">
              <view
                class="meal-bar-fill"
                :class="'meal-' + type"
                :style="{ width: dailyData.totalCalories > 0 ? (meal.calories / dailyData.totalCalories * 100) + '%' : '0%' }"
              />
            </view>
            <text class="meal-bar-val">{{ meal.calories }}kcal</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ========== 月统计 ========== -->
    <view v-if="activeTab === 'monthly' && monthlyData" class="stats-content">
      <!-- 核心指标卡片 -->
      <view class="indicator-cards">
        <view class="indicator-card">
          <text class="indicator-value">{{ monthlyData.avgDailyCalories }}</text>
          <text class="indicator-label">日均热量(kcal)</text>
        </view>
        <view class="indicator-card">
          <text class="indicator-value">{{ monthlyData.avgProteinRatio }}%</text>
          <text class="indicator-label">蛋白质供能比</text>
        </view>
        <view class="indicator-card">
          <text class="indicator-value">{{ monthlyData.avgFatRatio }}%</text>
          <text class="indicator-label">脂肪供能比</text>
        </view>
        <view class="indicator-card">
          <text class="indicator-value">{{ monthlyData.avgCarbsRatio }}%</text>
          <text class="indicator-label">碳水供能比</text>
        </view>
      </view>

      <!-- 每日热量趋势 -->
      <view class="card">
        <text class="card-title">每日热量趋势</text>
        <view class="trend-chart">
          <view
            v-for="(item, idx) in monthlyData.dailyTrend"
            :key="idx"
            class="trend-bar-col"
          >
            <view
              class="trend-bar"
              :style="{
                height: maxTrendCal > 0 ? (item.calories / maxTrendCal * 200) + 'rpx' : '0',
                backgroundColor: item.calories > (monthlyData.avgDailyCalories || 2000) ? '#FF6B6B' : '#7EC8A0'
              }"
            />
            <text class="trend-bar-date">{{ item.day }}</text>
          </view>
        </view>
      </view>

      <!-- 常吃食物 Top10 -->
      <view class="card">
        <text class="card-title">常吃食物 Top10</text>
        <view class="top-foods">
          <view
            v-for="(food, idx) in monthlyData.topFoods"
            :key="idx"
            class="top-food-item"
          >
            <text class="top-food-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</text>
            <text class="top-food-name">{{ food.foodName }}</text>
            <text class="top-food-count">{{ food.count }}次</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 日期选择器 -->
    <u-datetime-picker
      :show="showPicker"
      :value="pickerDate"
      :mode="activeTab === 'daily' ? 'date' : 'year-month'"
      @confirm="handlePickerConfirm"
      @cancel="showPicker = false"
      @close="showPicker = false"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { getDailySummary, getMonthlySummary } from '@/api'
import type { DailySummaryVO, MonthlySummaryVO } from '@/api/types'
import { formatDate, getToday } from '@/utils'

const tabs = [
  { label: '日统计', value: 'daily' },
  { label: '月统计', value: 'monthly' },
]
const activeTab = ref('daily')
const showPicker = ref(false)

// 日统计
const pickerDate = ref(new Date())
const currentDateStr = ref(formatDate(new Date()))
const dailyData = ref<DailySummaryVO | null>(null)

// 月统计
const currentMonthStr = computed(() => {
  const d = pickerDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})
const monthlyData = ref<MonthlySummaryVO | null>(null)

const maxTrendCal = computed(() => {
  if (!monthlyData.value?.dailyTrend) return 1
  return Math.max(...monthlyData.value.dailyTrend.map(i => i.calories), 1)
})

const mealTypeMap: Record<string, string> = {
  breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐',
}

async function loadDailyData() {
  try {
    const res = await getDailySummary(currentDateStr.value)
    dailyData.value = res.data
  } catch (e) { console.error(e) }
}

async function loadMonthlyData() {
  try {
    const d = pickerDate.value
    const res = await getMonthlySummary(d.getFullYear(), d.getMonth() + 1)
    monthlyData.value = res.data
  } catch (e) { console.error(e) }
}

function handlePickerConfirm(e: any) {
  pickerDate.value = new Date(e.value)
  if (activeTab.value === 'daily') {
    currentDateStr.value = formatDate(pickerDate.value)
  }
  showPicker.value = false
  activeTab.value === 'daily' ? loadDailyData() : loadMonthlyData()
}

watch(activeTab, (val) => {
  pickerDate.value = new Date()
  currentDateStr.value = formatDate(new Date())
  val === 'daily' ? loadDailyData() : loadMonthlyData()
})

onMounted(() => { loadDailyData() })
</script>

<style lang="scss" scoped>
.tab-bar { display: flex; background: #FFFFFF; margin: 16rpx 24rpx; border-radius: 16rpx; overflow: hidden; }
.tab-item { flex: 1; text-align: center; padding: 20rpx; font-size: 28rpx; color: #606266; }
.tab-active { color: #7EC8A0; font-weight: 600; background: #E8F4ED; }
.date-selector { display: flex; align-items: center; justify-content: center; gap: 8rpx; padding: 16rpx; }
.selector-text { font-size: 28rpx; color: #303133; }
.stats-content { padding-bottom: 120rpx; }
.card-title { font-size: 28rpx; font-weight: 600; color: #303133; display: block; margin-bottom: 16rpx; }
.ratio-chart { margin-top: 8rpx; }
.ratio-bar { display: flex; height: 24rpx; border-radius: 12rpx; overflow: hidden; }
.ratio-segment { height: 100%; }
.protein-seg { background: #FF6B6B; }
.fat-seg { background: #FFA94D; }
.carbs-seg { background: #4ECDC4; }
.ratio-legend { display: flex; justify-content: space-around; margin-top: 16rpx; }
.legend-item { display: flex; align-items: center; gap: 6rpx; font-size: 24rpx; color: #606266; }
.legend-dot { width: 12rpx; height: 12rpx; border-radius: 50%; }
.protein-dot { background: #FF6B6B; }
.fat-dot { background: #FFA94D; }
.carbs-dot { background: #4ECDC4; }
.meal-bars { display: flex; flex-direction: column; gap: 16rpx; }
.meal-bar-row { display: flex; align-items: center; gap: 12rpx; }
.meal-bar-label { width: 80rpx; font-size: 24rpx; color: #606266; }
.meal-bar-track { flex: 1; height: 20rpx; background: #F0F0F0; border-radius: 10rpx; overflow: hidden; }
.meal-bar-fill { height: 100%; border-radius: 10rpx; }
.meal-breakfast { background: #FFD93D; }
.meal-lunch { background: #6BCB77; }
.meal-dinner { background: #4D96FF; }
.meal-snack { background: #FF8B94; }
.meal-bar-val { width: 120rpx; font-size: 24rpx; color: #303133; text-align: right; }
.indicator-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; padding: 0 24rpx; margin-bottom: 16rpx; }
.indicator-card { background: #FFFFFF; border-radius: 16rpx; padding: 24rpx; text-align: center; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03); }
.indicator-value { font-size: 40rpx; font-weight: 700; color: #7EC8A0; display: block; }
.indicator-label { font-size: 22rpx; color: #909399; margin-top: 4rpx; display: block; }
.trend-chart { display: flex; align-items: flex-end; gap: 6rpx; height: 260rpx; padding: 0 8rpx; overflow-x: auto; }
.trend-bar-col { display: flex; flex-direction: column; align-items: center; flex: 1; min-width: 28rpx; }
.trend-bar { width: 24rpx; border-radius: 6rpx 6rpx 0 0; }
.trend-bar-date { font-size: 18rpx; color: #C0C4CC; margin-top: 4rpx; }
.top-foods { display: flex; flex-direction: column; gap: 12rpx; }
.top-food-item { display: flex; align-items: center; gap: 12rpx; }
.top-food-rank { width: 36rpx; height: 36rpx; border-radius: 50%; text-align: center; line-height: 36rpx; font-size: 22rpx; color: #FFFFFF; background: #C0C4CC; }
.rank-1 { background: #FFD93D; }
.rank-2 { background: #C0C4CC; }
.rank-3 { background: #FFA94D; }
.top-food-name { flex: 1; font-size: 26rpx; color: #303133; }
.top-food-count { font-size: 24rpx; color: #909399; }
</style>
