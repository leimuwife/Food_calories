<template>
  <view class="food-card" @tap="$emit('click')">
    <view class="food-info">
      <view class="food-top">
        <text class="food-name">{{ food.foodName }}</text>
        <view class="food-category" :style="{ backgroundColor: categoryColor + '20', color: categoryColor }">
          {{ food.category || '其他' }}
        </view>
      </view>
      <text class="food-calorie">{{ food.caloriesPer100g }} kcal/100g</text>
      <view class="food-nutrients">
        <text class="nutrient-tag protein-tag">蛋白质 {{ food.proteinPer100g }}g</text>
        <text class="nutrient-tag fat-tag">脂肪 {{ food.fatPer100g }}g</text>
        <text class="nutrient-tag carbs-tag">碳水 {{ food.carbsPer100g }}g</text>
      </view>
    </view>
    <view class="food-action">
      <u-icon name="arrow-right" size="16" color="#C0C4CC" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FoodVO } from '@/api/types'
import { CATEGORY_COLORS } from '@/utils'

const props = defineProps<{
  food: FoodVO
}>()

defineEmits<{
  click: []
}>()

const categoryColor = computed(() => CATEGORY_COLORS[props.food.category || '其他'] || '#ADB5BD')
</script>

<style lang="scss" scoped>
.food-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

.food-info {
  flex: 1;
}

.food-top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.food-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}

.food-category {
  font-size: 22rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
}

.food-calorie {
  font-size: 26rpx;
  color: #7EC8A0;
  font-weight: 500;
  margin-bottom: 8rpx;
  display: block;
}

.food-nutrients {
  display: flex;
  gap: 16rpx;
}

.nutrient-tag {
  font-size: 22rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  background: #F5F7FA;
  color: #909399;
}

.protein-tag {
  color: #FF6B6B;
  background: #FFF0F0;
}

.fat-tag {
  color: #FFA94D;
  background: #FFF4E6;
}

.carbs-tag {
  color: #4ECDC4;
  background: #E8FAF8;
}

.food-action {
  margin-left: 16rpx;
}
</style>
