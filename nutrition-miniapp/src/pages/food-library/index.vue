<template>
  <view class="page-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <u-search
        v-model="keyword"
        placeholder="搜索食物名称"
        :show-action="false"
        @search="doSearch"
        @change="onKeywordChange"
      />
    </view>

    <!-- 分类筛选 -->
    <scroll-view scroll-x class="category-scroll">
      <view class="category-list">
        <view
          v-for="cat in categories"
          :key="cat"
          class="category-item"
          :class="{ 'category-active': activeCategory === cat }"
          @tap="selectCategory(cat)"
        >
          <text>{{ cat }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 食物列表 -->
    <scroll-view scroll-y class="food-list" @scrolltolower="loadMore">
      <FoodCard
        v-for="food in foodList"
        :key="food.foodId || food.id"
        :food="food"
        @click="showFoodDetail(food)"
      />

      <view v-if="loading" class="loading-state">
        <u-loading-icon text="搜索中..." />
      </view>

      <EmptyState
        v-if="!loading && foodList.length === 0"
        title="未找到食物"
        :description="keyword ? '换个关键词试试' : '请在搜索框输入食物名称'"
      />
    </scroll-view>

    <!-- 食物详情弹窗 -->
    <u-popup :show="showDetail" mode="bottom" :round="20" @close="showDetail = false">
      <view class="detail-dialog" v-if="detailFood">
        <text class="detail-name">{{ detailFood.foodName }}</text>
        <view class="detail-category-tag">{{ detailFood.category }}</view>

        <view class="detail-nutrition-grid">
          <view class="detail-n-item">
            <text class="detail-n-value">{{ detailFood.caloriesPer100g }}</text>
            <text class="detail-n-label">热量(kcal)</text>
          </view>
          <view class="detail-n-item">
            <text class="detail-n-value protein-text">{{ detailFood.proteinPer100g }}</text>
            <text class="detail-n-label">蛋白质(g)</text>
          </view>
          <view class="detail-n-item">
            <text class="detail-n-value fat-text">{{ detailFood.fatPer100g }}</text>
            <text class="detail-n-label">脂肪(g)</text>
          </view>
          <view class="detail-n-item">
            <text class="detail-n-value carbs-text">{{ detailFood.carbsPer100g }}</text>
            <text class="detail-n-label">碳水(g)</text>
          </view>
        </view>

        <text class="detail-per100">* 以上数据为每100g可食部分营养含量</text>

        <view class="detail-add-section">
          <view class="weight-row">
            <text class="weight-label">食用重量</text>
            <view class="weight-control">
              <u-icon name="minus-circle" size="28" color="#7EC8A0" @tap="detailWeight > 10 && (detailWeight -= 10)" />
              <u-input v-model="detailWeight" type="number" :custom-style="{ width: '120rpx', textAlign: 'center' }" />
              <text class="weight-unit">g</text>
              <u-icon name="plus-circle" size="28" color="#7EC8A0" @tap="detailWeight += 10" />
            </view>
          </view>

          <view class="estimate-row" v-if="detailWeight > 0">
            <text>预估摄入：{{ Math.round(detailFood.caloriesPer100g * detailWeight / 100) }}kcal</text>
          </view>

          <view class="meal-select-row">
            <text class="meal-label">餐次：</text>
            <u-radio-group v-model="detailMealType" placement="row">
              <u-radio v-for="opt in mealOptions" :key="opt.value" :label="opt.label" :name="opt.value" :custom-style="{ marginRight: '16rpx' }" />
            </u-radio-group>
          </view>

          <u-button
            type="primary"
            :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx', marginTop: '20rpx' }"
            text="添加到餐食"
            @tap="handleAddFromDetail"
          />
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { searchFood, getFoodDetail, addDietRecord, getFoodCategories } from '@/api'
import { getToday, MEAL_OPTIONS } from '@/utils'
import FoodCard from '@/components/FoodCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const keyword = ref('')
const categories = ref<string[]>([])
const activeCategory = ref('')
const foodList = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const totalCount = ref(0)

// 详情弹窗
const showDetail = ref(false)
const detailFood = ref<any>(null)
const detailWeight = ref(100)
const detailMealType = ref('lunch')
const mealOptions = MEAL_OPTIONS

function selectCategory(cat: string) {
  activeCategory.value = activeCategory.value === cat ? '' : cat
  page.value = 1
  doSearch()
}

function onKeywordChange(val: string) {
  keyword.value = val
  // 防抖搜索
  if ((window as any).__searchTimer) clearTimeout((window as any).__searchTimer)
  ;(window as any).__searchTimer = setTimeout(() => doSearch(), 300)
}

async function doSearch() {
  loading.value = true
  try {
    const res = await searchFood(keyword.value.trim(), activeCategory.value || undefined)
    foodList.value = res.data.list || []
    totalCount.value = res.data.total || 0
  } catch (e) {
    console.error('searchFood error:', e)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (foodList.value.length >= totalCount.value) return
  page.value++
  try {
    const res = await searchFood(keyword.value.trim(), activeCategory.value || undefined)
    const more = res.data.list || []
    foodList.value.push(...more)
  } catch (e) { /* ignore */ }
}

async function showFoodDetail(food: any) {
  try {
    const id = food.foodId || food.id
    const res = await getFoodDetail(id)
    detailFood.value = res.data || food
  } catch (e) {
    detailFood.value = food
  }
  detailWeight.value = 100
  showDetail.value = true
}

async function handleAddFromDetail() {
  if (!detailFood.value) return
  try {
    const food = detailFood.value
    await addDietRecord({
      recordDate: getToday(),
      mealType: detailMealType.value,
      items: [{
        foodId: food.foodId || food.id,
        foodName: food.foodName,
        weight: detailWeight.value,
      }],
    })
    uni.showToast({ title: '添加成功', icon: 'success' })
    showDetail.value = false
  } catch (e) { /* handled in api */ }
}

async function loadCategories() {
  try {
    const res = await getFoodCategories()
    categories.value = res.data.map((c: any) => c.category)
  } catch (e) {
    console.error('loadCategories error:', e)
  }
}

onMounted(() => {
  loadCategories()
  doSearch()
})
</script>

<style lang="scss" scoped>
.search-bar { padding: 16rpx 24rpx; }
.category-scroll { white-space: nowrap; padding: 0 24rpx 16rpx; }
.category-list { display: inline-flex; gap: 16rpx; }
.category-item {
  padding: 10rpx 24rpx;
  border-radius: 32rpx;
  background: #F5F7FA;
  font-size: 26rpx;
  color: #606266;
}
.category-active { background: #7EC8A0; color: #FFFFFF; }
.food-list { padding: 0 24rpx 120rpx; }
.loading-state { display: flex; justify-content: center; padding: 60rpx; }
.detail-dialog { padding: 32rpx 24rpx 48rpx; }
.detail-name { font-size: 34rpx; font-weight: 600; color: #303133; display: block; text-align: center; }
.detail-category-tag {
  display: inline-block;
  font-size: 22rpx;
  color: #7EC8A0;
  background: #E8F4ED;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  margin: 12rpx auto;
  text-align: center;
}
.detail-nutrition-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin: 24rpx 0;
}
.detail-n-item {
  background: #F5F7FA;
  border-radius: 16rpx;
  padding: 20rpx;
  text-align: center;
}
.detail-n-value { font-size: 36rpx; font-weight: 700; color: #7EC8A0; display: block; }
.protein-text { color: #FF6B6B; }
.fat-text { color: #FFA94D; }
.carbs-text { color: #4ECDC4; }
.detail-n-label { font-size: 22rpx; color: #909399; margin-top: 4rpx; display: block; }
.detail-per100 { font-size: 22rpx; color: #C0C4CC; display: block; text-align: center; margin-bottom: 20rpx; }
.detail-add-section { border-top: 1rpx solid #F0F0F0; padding-top: 20rpx; }
.weight-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.weight-control { display: flex; align-items: center; gap: 12rpx; }
.weight-unit { font-size: 26rpx; color: #606266; }
.estimate-row { font-size: 26rpx; color: #7EC8A0; margin-bottom: 16rpx; }
.meal-select-row { display: flex; align-items: center; }
.meal-label { font-size: 28rpx; color: #303133; margin-right: 12rpx; }
</style>
