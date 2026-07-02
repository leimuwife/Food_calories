<template>
  <view class="page-container">
    <!-- 日期选择 -->
    <view class="date-bar">
      <u-icon name="arrow-left" size="18" color="#7EC8A0" @tap="prevDay" />
      <text class="date-text" @tap="showDatePicker = true">{{ displayDate }}</text>
      <u-icon name="arrow-right" size="18" color="#7EC8A0" @tap="nextDay" />
    </view>

    <!-- 营养进度卡片 -->
    <NutritionProgress
      v-if="summary"
      :total-calories="summary.totalCalories"
      :total-protein="summary.totalProtein"
      :total-fat="summary.totalFat"
      :total-carbs="summary.totalCarbs"
      :calorie-goal="summary.calorieGoal"
      :protein-goal="summary.proteinGoal"
      :fat-goal="summary.fatGoal"
      :carbs-goal="summary.carbsGoal"
    />

    <!-- 快捷添加按钮 -->
    <view class="quick-add">
      <u-button
        type="primary"
        :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx' }"
        icon="plus"
        text="  添加食物"
        @tap="showAddDialog = true"
      />
    </view>

    <!-- 餐食列表 -->
    <view class="meals-section" v-if="summary && summary.meals">
      <view
        v-for="(meal, type) in summary.meals"
        :key="type"
        class="meal-group"
        v-show="meal.items && meal.items.length > 0"
      >
        <view class="meal-header">
          <view class="meal-title-row">
            <view :class="['meal-dot', 'dot-' + type]" />
            <text class="meal-type">{{ mealTypeName(type as string) }}</text>
          </view>
          <text class="meal-calories">{{ meal.calories }} kcal</text>
        </view>

        <view v-for="item in meal.items" :key="item.id" class="meal-item">
          <view class="meal-item-info">
            <text class="meal-item-name">{{ item.foodName }}</text>
            <text class="meal-item-weight">{{ item.weight }}g</text>
          </view>
          <view class="meal-item-right">
            <text class="meal-item-cal">{{ item.calories }}kcal</text>
            <u-icon name="trash" size="16" color="#C0C4CC" @tap="handleDeleteItem(item.id!)" />
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <EmptyState
        v-if="!hasMeals"
        title="今天还没记录"
        description="点击上方按钮添加食物吧"
      />
    </view>

    <!-- 添加食物弹窗 -->
    <u-popup :show="showAddDialog" mode="bottom" :round="20" @close="showAddDialog = false">
      <view class="add-dialog">
        <text class="dialog-title">添加食物</text>

        <!-- 搜索食物 -->
        <view class="search-box">
          <u-search
            v-model="searchKeyword"
            placeholder="搜索食物名称"
            :show-action="false"
            @search="handleSearch"
            @change="handleSearch"
          />
        </view>

        <!-- 搜索结果 -->
        <scroll-view scroll-y class="search-results" v-if="searchResults.length > 0">
          <view
            v-for="food in searchResults"
            :key="food.foodId || food.id"
            class="search-item"
            :class="{ 'search-item-active': selectedFood && (selectedFood.foodId || selectedFood.id) === (food.foodId || food.id) }"
            @tap="selectFood(food)"
          >
            <view class="search-item-info">
              <text class="search-item-name">{{ food.foodName }}</text>
              <text class="search-item-cal">{{ food.caloriesPer100g }} kcal/100g</text>
            </view>
          </view>
        </scroll-view>

        <!-- 已选食物 & 重量 -->
        <view class="add-form" v-if="selectedFood">
          <text class="selected-food">已选：{{ selectedFood.foodName }}</text>
          <view class="weight-input-row">
            <text class="weight-label">食用重量(g)</text>
            <view class="weight-control">
              <u-icon name="minus-circle" size="28" color="#7EC8A0" @tap="decreaseWeight" />
              <u-input
                v-model="foodWeight"
                type="number"
                :custom-style="{ width: '120rpx', textAlign: 'center' }"
              />
              <u-icon name="plus-circle" size="28" color="#7EC8A0" @tap="increaseWeight" />
            </view>
          </view>

          <!-- 预估营养 -->
          <view class="estimate-nutrition" v-if="foodWeight > 0">
            <text class="estimate-text">
              预估：{{ estimatedCalories }}kcal | 蛋白{{ estimatedProtein }}g | 脂肪{{ estimatedFat }}g | 碳水{{ estimatedCarbs }}g
            </text>
          </view>

          <!-- 选择餐次 -->
          <view class="meal-select-row">
            <text class="meal-select-label">餐次：</text>
            <u-radio-group v-model="selectedMealType" placement="row">
              <u-radio
                v-for="opt in mealOptions"
                :key="opt.value"
                :label="opt.label"
                :name="opt.value"
                :custom-style="{ marginRight: '20rpx' }"
              />
            </u-radio-group>
          </view>

          <!-- 确认添加 -->
          <u-button
            type="primary"
            :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx', marginTop: '24rpx' }"
            text="确认添加"
            @tap="handleAddFood"
          />
        </view>
      </view>
    </u-popup>

    <!-- 日期选择器 -->
    <u-datetime-picker
      :show="showDatePicker"
      :value="currentDate"
      mode="date"
      @confirm="handleDateConfirm"
      @cancel="showDatePicker = false"
      @close="showDatePicker = false"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onShow } from 'vue'
import { useUserStore } from '@/stores/user'
import { useDietStore, type DietItem } from '@/stores/diet'
import { getTodayRecords, addDietRecord, deleteDietItem, searchFood } from '@/api'
import type { DailySummaryVO, FoodVO } from '@/api/types'
import { formatDate, getToday, formatDateDisplay, MEAL_OPTIONS, calcActualIntake } from '@/utils'
import NutritionProgress from '@/components/NutritionProgress.vue'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()
const dietStore = useDietStore()

// 日期
const currentDate = ref(new Date())
const displayDate = computed(() => formatDateDisplay(formatDate(currentDate.value)))
const showDatePicker = ref(false)

// 饮食数据
const summary = ref<DailySummaryVO | null>(null)
const hasMeals = computed(() => {
  if (!summary.value?.meals) return false
  return Object.values(summary.value.meals).some(m => m.items && m.items.length > 0)
})

// 添加食物弹窗
const showAddDialog = ref(false)
const searchKeyword = ref('')
const searchResults = ref<FoodVO[]>([])
const selectedFood = ref<FoodVO | null>(null)
const foodWeight = ref(100)
const selectedMealType = ref('lunch')
const mealOptions = MEAL_OPTIONS

// 预估营养
const estimatedCalories = computed(() => {
  if (!selectedFood.value) return 0
  return Math.round(selectedFood.value.caloriesPer100g * foodWeight.value / 100)
})
const estimatedProtein = computed(() => {
  if (!selectedFood.value) return 0
  return Math.round(selectedFood.value.proteinPer100g * foodWeight.value / 100 * 10) / 10
})
const estimatedFat = computed(() => {
  if (!selectedFood.value) return 0
  return Math.round(selectedFood.value.fatPer100g * foodWeight.value / 100 * 10) / 10
})
const estimatedCarbs = computed(() => {
  if (!selectedFood.value) return 0
  return Math.round(selectedFood.value.carbsPer100g * foodWeight.value / 100 * 10) / 10
})

// 加载今日数据
async function loadTodayData() {
  try {
    const date = formatDate(currentDate.value)
    const res = await getTodayRecords(date)
    if (res.data) {
      summary.value = res.data.summary
    }
  } catch (e) {
    console.error('loadTodayData error:', e)
  }
}

// 日期切换
function prevDay() {
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() - 1)
  currentDate.value = d
  loadTodayData()
}
function nextDay() {
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() + 1)
  currentDate.value = d
  loadTodayData()
}
function handleDateConfirm(e: any) {
  currentDate.value = new Date(e.value)
  showDatePicker.value = false
  loadTodayData()
}

// 搜索食物
let searchTimer: any = null
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchFood(searchKeyword.value.trim())
      searchResults.value = res.data.list || []
    } catch (e) {
      console.error('searchFood error:', e)
    }
  }, 300)
}

// 选择食物
function selectFood(food: any) {
  selectedFood.value = food
}

// 重量加减
function increaseWeight() { foodWeight.value += 10 }
function decreaseWeight() { if (foodWeight.value > 10) foodWeight.value -= 10 }

// 添加食物
async function handleAddFood() {
  if (!selectedFood.value) {
    uni.showToast({ title: '请先选择食物', icon: 'none' })
    return
  }
  try {
    await addDietRecord({
      recordDate: formatDate(currentDate.value),
      mealType: selectedMealType.value,
      items: [{
        foodId: selectedFood.value.foodId || selectedFood.value.id,
        foodName: selectedFood.value.foodName,
        weight: foodWeight.value,
      }],
    })
    uni.showToast({ title: '添加成功', icon: 'success' })
    showAddDialog.value = false
    resetAddForm()
    loadTodayData()
  } catch (e) {
    console.error('handleAddFood error:', e)
  }
}

function resetAddForm() {
  searchKeyword.value = ''
  searchResults.value = []
  selectedFood.value = null
  foodWeight.value = 100
}

// 删除条目
async function handleDeleteItem(itemId: number) {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这条记录吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteDietItem(itemId)
          uni.showToast({ title: '已删除', icon: 'success' })
          loadTodayData()
        } catch (e) { /* handled in api */ }
      }
    },
  })
}

function mealTypeName(type: string) {
  const map: Record<string, string> = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' }
  return map[type] || type
}

onShow(() => { loadTodayData() })
</script>

<style lang="scss" scoped>
.date-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32rpx;
  padding: 20rpx 0;
}
.date-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
}
.quick-add {
  padding: 0 24rpx;
  margin-bottom: 16rpx;
}
.meals-section {
  padding: 0 24rpx 120rpx;
}
.meal-group {
  background: #FFFFFF;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
}
.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #F0F0F0;
}
.meal-title-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.meal-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.dot-breakfast { background: #FFD93D; }
.dot-lunch { background: #6BCB77; }
.dot-dinner { background: #4D96FF; }
.dot-snack { background: #FF8B94; }
.meal-type { font-size: 28rpx; font-weight: 600; color: #303133; }
.meal-calories { font-size: 26rpx; color: #7EC8A0; font-weight: 500; }
.meal-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
}
.meal-item-info { display: flex; flex-direction: column; gap: 4rpx; }
.meal-item-name { font-size: 28rpx; color: #303133; }
.meal-item-weight { font-size: 24rpx; color: #909399; }
.meal-item-right { display: flex; align-items: center; gap: 16rpx; }
.meal-item-cal { font-size: 26rpx; color: #606266; }
.add-dialog { padding: 32rpx 24rpx 48rpx; }
.dialog-title { font-size: 32rpx; font-weight: 600; color: #303133; display: block; text-align: center; margin-bottom: 24rpx; }
.search-box { margin-bottom: 16rpx; }
.search-results { max-height: 400rpx; }
.search-item { padding: 20rpx; border-bottom: 1rpx solid #F0F0F0; }
.search-item-active { background: #E8F4ED; }
.search-item-info { display: flex; justify-content: space-between; }
.search-item-name { font-size: 28rpx; color: #303133; }
.search-item-cal { font-size: 24rpx; color: #7EC8A0; }
.add-form { margin-top: 20rpx; }
.selected-food { font-size: 26rpx; color: #7EC8A0; font-weight: 500; display: block; margin-bottom: 16rpx; }
.weight-input-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.weight-label { font-size: 28rpx; color: #303133; }
.weight-control { display: flex; align-items: center; gap: 16rpx; }
.estimate-nutrition { background: #F5F7FA; border-radius: 12rpx; padding: 16rpx; margin-bottom: 16rpx; }
.estimate-text { font-size: 24rpx; color: #606266; }
.meal-select-row { display: flex; align-items: center; margin-top: 16rpx; }
.meal-select-label { font-size: 28rpx; color: #303133; margin-right: 12rpx; }
</style>
