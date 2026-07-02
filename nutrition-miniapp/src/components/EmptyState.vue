<template>
  <view class="empty-state">
    <image class="empty-image" :src="image" mode="aspectFit" />
    <text class="empty-title">{{ title }}</text>
    <text class="empty-desc">{{ description }}</text>
    <slot name="action" />
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
  image?: string
}>()
</script>

<script lang="ts">
export default {
  defaultProps: {
    title: '暂无数据',
    description: '还没有任何记录哦',
    image: '/static/images/empty.png',
  },
}
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 40rpx;
}

.empty-image {
  width: 240rpx;
  height: 240rpx;
  margin-bottom: 24rpx;
  opacity: 0.6;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #909399;
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #C0C4CC;
  text-align: center;
}
</style>
