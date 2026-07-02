<template>
  <view class="page-container">
    <!-- 日历选择 -->
    <view class="calendar-card card">
      <u-calendar
        :show="showCalendar"
        :default-date="selectedDate"
        @confirm="handleCalendarConfirm"
        @close="showCalendar = false"
      />
      <view class="date-header" @tap="showCalendar = true">
        <u-icon name="calendar" size="20" color="#7EC8A0" />
        <text class="date-header-text">{{ selectedDate }}</text>
        <u-icon name="arrow-down" size="14" color="#909399" />
      </view>
    </view>

    <!-- 关键词搜索 -->
    <view class="search-row">
      <u-search
        v-model="searchKey"
        placeholder="搜索历史食物"
        :show-action="false"
        @search="doSearchHistory"
        @change="onSearchChange"
      />
    </view>

    <!-- 历史记录列表 -->
    <scroll-view scroll-y class="history-list">
      <view v-if="records.length > 0">
        <view v-for="(record, idx) in records" :key="record.id || idx" class="card history-card">
          <view class="history-header">
            <view class="history-meta">
              <text class="history-meal" :class="'tag-' + record.mealType">{{ mealMap[record.mealType] || record.mealType }}</text>
              <text class="history-date">{{ record.recordDate }}</text>
            </view>
            <view class="history-actions">
              <text class="action-copy" @tap="handleCopyToday(record)">复制到今日</text>
              <u-icon name="trash" size="16" color="#C0C4CC" @tap="handleDelete(record.id!)" />
            </view>
          </view>
          <view class="history-items">
            <view v-for="item in record.items" :key="item.id" class="history-item">
              <text class="hi-name">{{ item.foodName }}</text>
              <text class="hi-weight">{{ item.weight }}g</text>
              <text class="hi-cal">{{ item.calories }}kcal</text>
            </view>
          </view>
          <view class="history-total" v-if="record.items && record.items.length > 0">
            <text>合计：{{ recordTotalCal(record) }}kcal</text>
          </view>
        </view>
      </view>

      <EmptyState
        v-else
        title="暂无记录"
        :description="searchKey ? '未找到匹配记录' : '选择日期查看历史饮食'"
      />
    </scroll-view>

    <!-- 日历组件 -->
    <u-popup :show="showCalendar" mode="bottom" :round="20" @close="showCalendar = false">
      <view class="calendar-popup">
        <u-calendar
          :show="true"
          :default-date="selectedDate"
          @confirm="handleCalendarConfirm"
        />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRecordsByRange, deleteDietRecord, copyRecordToToday } from '@/api'
import type { DietRecordVO } from '@/api/types'
import { getToday, formatDate } from '@/utils'
import EmptyState from '@/components/EmptyState.vue'

const mealMap: Record<string, string> = {
  breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐',
}

const showCalendar = ref(false)
const selectedDate = ref(getToday())
const searchKey = ref('')
const records = ref<DietRecordVO[]>([])
let allRecords: DietRecordVO[] = []

async function loadRecords() {
  try {
    const res = await getRecordsByRange(selectedDate.value, selectedDate.value)
    allRecords = res.data || []
    applyFilter()
  } catch (e) { console.error(e) }
}

function applyFilter() {
  if (!searchKey.value.trim()) {
    records.value = allRecords
  } else {
    const kw = searchKey.value.trim().toLowerCase()
    records.value = allRecords.filter(r =>
      r.items?.some(i => i.foodName.toLowerCase().includes(kw))
    )
  }
}

function handleCalendarConfirm(e: any) {
  selectedDate.value = formatDate(new Date(e.value || e))
  showCalendar.value = false
  loadRecords()
}

function onSearchChange(val: string) {
  searchKey.value = val
  if ((window as any).__histTimer) clearTimeout((window as any).__histTimer)
  ;(window as any).__histTimer = setTimeout(() => applyFilter(), 300)
}

function doSearchHistory() { applyFilter() }

function recordTotalCal(record: DietRecordVO) {
  return record.items?.reduce((sum, i) => sum + (i.calories || 0), 0) || 0
}

async function handleCopyToday(record: DietRecordVO) {
  try {
    await copyRecordToToday(record.id, getToday())
    uni.showToast({ title: '已复制到今日', icon: 'success' })
  } catch (e) { /* handled */ }
}

async function handleDelete(recordId: number) {
  uni.showModal({
    title: '确认删除',
    content: '删除后无法恢复',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteDietRecord(recordId)
          uni.showToast({ title: '已删除', icon: 'success' })
          loadRecords()
        } catch (e) { /* handled */ }
      }
    },
  })
}

onMounted(() => { loadRecords() })
</script>

<style lang="scss" scoped>
.date-header { display: flex; align-items: center; gap: 8rpx; padding: 8rpx 0; }
.date-header-text { font-size: 28rpx; font-weight: 600; color: #303133; }
.search-row { padding: 0 24rpx 16rpx; }
.history-list { padding: 0 24rpx 120rpx; }
.history-card { margin: 0 0 16rpx; }
.history-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.history-meta { display: flex; align-items: center; gap: 12rpx; }
.history-meal { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.history-date { font-size: 24rpx; color: #909399; }
.history-actions { display: flex; align-items: center; gap: 20rpx; }
.action-copy { font-size: 24rpx; color: #7EC8A0; }
.history-items { border-top: 1rpx solid #F0F0F0; padding-top: 12rpx; }
.history-item { display: flex; align-items: center; padding: 8rpx 0; }
.hi-name { flex: 1; font-size: 26rpx; color: #303133; }
.hi-weight { font-size: 24rpx; color: #909399; margin: 0 16rpx; }
.hi-cal { font-size: 24rpx; color: #7EC8A0; }
.history-total { border-top: 1rpx solid #F0F0F0; padding-top: 12rpx; margin-top: 8rpx; font-size: 26rpx; color: #303133; text-align: right; font-weight: 500; }
.calendar-popup { padding: 24rpx; }
</style>
