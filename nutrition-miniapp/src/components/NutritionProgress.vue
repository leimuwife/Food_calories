<template>
  <view class="nutrition-progress">
    <!-- 热量环形进度 -->
    <view class="calorie-section">
      <view class="calorie-ring">
        <u-circle-progress
          :percent="caloriePercent"
          :border-width="12"
          :width="180"
          :height="180"
          active-color="#7EC8A0"
          inactive-color="#E8F4ED"
        >
          <view class="ring-content">
            <text class="calorie-value">{{ totalCalories }}</text>
            <text class="calorie-unit">kcal</text>
            <text class="calorie-goal">目标 {{ calorieGoal }}</text>
          </view>
        </u-circle-progress>
      </view>
    </view>

    <!-- 三大营养素进度条 -->
    <view class="nutrients-section">
      <view class="nutrient-item">
        <view class="nutrient-header">
          <view class="nutrient-label">
            <view class="nutrient-dot protein-dot" />
            <text class="nutrient-name">蛋白质</text>
          </view>
          <text class="nutrient-value">{{ totalProtein }} / {{ proteinGoal }}g</text>
        </view>
        <u-line-progress
          :percent="proteinPercent"
          active-color="#FF6B6B"
          inactive-color="#FFE8E8"
          :height="8"
          :show-percent="false"
          round
        />
      </view>

      <view class="nutrient-item">
        <view class="nutrient-header">
          <view class="nutrient-label">
            <view class="nutrient-dot fat-dot" />
            <text class="nutrient-name">脂肪</text>
          </view>
          <text class="nutrient-value">{{ totalFat }} / {{ fatGoal }}g</text>
        </view>
        <u-line-progress
          :percent="fatPercent"
          active-color="#FFA94D"
          inactive-color="#FFF0E0"
          :height="8"
          :show-percent="false"
          round
        />
      </view>

      <view class="nutrient-item">
        <view class="nutrient-header">
          <view class="nutrient-label">
            <view class="nutrient-dot carbs-dot" />
            <text class="nutrient-name">碳水</text>
          </view>
          <text class="nutrient-value">{{ totalCarbs }} / {{ carbsGoal }}g</text>
        </view>
        <u-line-progress
          :percent="carbsPercent"
          active-color="#4ECDC4"
          inactive-color="#E0FAF8"
          :height="8"
          :show-percent="false"
          round
        />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
  calorieGoal: number
  proteinGoal: number
  fatGoal: number
  carbsGoal: number
}>()

const caloriePercent = computed(() =>
  props.calorieGoal > 0 ? Math.min(Math.round((props.totalCalories / props.calorieGoal) * 100), 100) : 0
)
const proteinPercent = computed(() =>
  props.proteinGoal > 0 ? Math.min(Math.round((props.totalProtein / props.proteinGoal) * 100), 100) : 0
)
const fatPercent = computed(() =>
  props.fatGoal > 0 ? Math.min(Math.round((props.totalFat / props.fatGoal) * 100), 100) : 0
)
const carbsPercent = computed(() =>
  props.carbsGoal > 0 ? Math.min(Math.round((props.totalCarbs / props.carbsGoal) * 100), 100) : 0
)
</script>

<style lang="scss" scoped>
.nutrition-progress {
  background: linear-gradient(135deg, #7EC8A0 0%, #5BA07A 100%);
  border-radius: 24rpx;
  padding: 32rpx;
  margin: 20rpx 24rpx;
}

.calorie-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24rpx;
}

.calorie-ring {
  :deep(.u-circle-progress) {
    justify-content: center;
  }
}

.ring-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.calorie-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #FFFFFF;
  line-height: 1.2;
}

.calorie-unit {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.calorie-goal {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4rpx;
}

.nutrients-section {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.nutrient-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.nutrient-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nutrient-label {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.nutrient-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}

.protein-dot {
  background-color: #FF6B6B;
}

.fat-dot {
  background-color: #FFA94D;
}

.carbs-dot {
  background-color: #4ECDC4;
}

.nutrient-name {
  font-size: 26rpx;
  color: #FFFFFF;
}

.nutrient-value {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}
</style>
