<template>
  <view class="page-container">
    <view class="header-area">
      <view class="header-title-wrap">
        <svg viewBox="0 0 64 64" class="header-icon">
          <circle cx="32" cy="32" r="28" fill="#FFB6C1"/>
          <circle cx="24" cy="28" r="4" fill="#333"/>
          <circle cx="40" cy="28" r="4" fill="#333"/>
          <circle cx="25" cy="27" r="1.5" fill="#fff"/>
          <circle cx="41" cy="27" r="1.5" fill="#fff"/>
          <path d="M32 36 Q30 40 32 44 Q34 40 32 36" stroke="#333" stroke-width="2" fill="none"/>
          <rect x="38" y="18" width="14" height="12" rx="2" fill="#FF69B4"/>
          <text x="45" y="27" font-size="8" fill="#fff" text-anchor="middle">OK</text>
        </svg>
        <text class="header-title">减肥打卡</text>
      </view>
      <view class="header-divider"></view>
    </view>

    <view class="calendar-card">
      <view class="calendar-header">
        <view class="year-nav">
          <view class="nav-btn-lg" @tap="prevYear">
            <svg viewBox="0 0 48 48" class="nav-icon-lg">
              <path d="M24 32 L24 16 M16 24 L32 24" stroke="#FF69B4" stroke-width="4" fill="none" stroke-linecap="round"/>
            </svg>
          </view>
          <view class="nav-btn-lg" @tap="nextYear">
            <svg viewBox="0 0 48 48" class="nav-icon-lg">
              <path d="M24 16 L24 32 M16 24 L32 24" stroke="#FF69B4" stroke-width="4" fill="none" stroke-linecap="round"/>
            </svg>
          </view>
        </view>

        <view class="month-select-btn" @tap="showMonthPicker = true">
          <text class="month-text">{{ currentYear }}年{{ currentMonth }}月</text>
          <svg viewBox="0 0 48 48" class="arrow-icon">
            <path d="M16 16 L32 24 L16 32" stroke="#FF69B4" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </view>

        <view class="month-nav">
          <view class="nav-btn-lg" @tap="prevMonth">
            <svg viewBox="0 0 48 48" class="nav-icon-lg">
              <path d="M28 16 L16 24 L28 32" stroke="#FF69B4" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </view>
          <view class="nav-btn-lg" @tap="nextMonth">
            <svg viewBox="0 0 48 48" class="nav-icon-lg">
              <path d="M20 16 L32 24 L20 32" stroke="#FF69B4" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </view>
        </view>
      </view>

      <view class="weekday-row">
        <text v-for="day in weekdays" :key="day" class="weekday">{{ day }}</text>
      </view>

      <view class="calendar-grid">
        <view 
          v-for="(date, index) in calendarDates" 
          :key="index"
          :class="getDateClass(date)"
          @tap="handleDateTap(date)"
        >
          <text v-if="date.isCurrentMonth" class="date-text">{{ date.day }}</text>
        </view>
      </view>

      <view class="calendar-hint">
        <text class="hint-text">选中日期代表当日减肥打卡，未选中代表当日放纵餐不打卡</text>
      </view>
    </view>

    <view v-if="showMonthPicker" class="picker-overlay" @tap="showMonthPicker = false">
      <view class="picker-popup" @tap.stop>
        <view class="picker-header">
          <text class="picker-title">选择年月</text>
          <view class="picker-close" @tap="showMonthPicker = false">
            <svg viewBox="0 0 48 48" class="close-icon">
              <path d="M16 16 L32 32 M32 16 L16 32" stroke="#999" stroke-width="3" fill="none" stroke-linecap="round"/>
            </svg>
          </view>
        </view>
        <view class="picker-content">
          <scroll-view 
            scroll-y 
            class="picker-column"
            :scroll-into-view="yearScrollId"
            scroll-with-animation
          >
            <view v-for="year in yearOptions" :key="year" :id="'year-' + year" class="picker-item" :class="{ 'picker-item-active': year === pickerYear }" @tap="pickerYear = year">
              <text class="picker-text">{{ year }}年</text>
            </view>
          </scroll-view>
          <view class="picker-divider"></view>
          <scroll-view 
            scroll-y 
            class="picker-column"
            :scroll-into-view="monthScrollId"
            scroll-with-animation
          >
            <view v-for="month in monthOptions" :key="month.value" :id="'month-' + month.value" class="picker-item" :class="{ 'picker-item-active': month.value === pickerMonth }" @tap="pickerMonth = month.value">
              <text class="picker-text">{{ month.label }}</text>
            </view>
          </scroll-view>
        </view>
        <view class="picker-footer">
          <view class="picker-confirm" @tap="confirmMonthPicker">
            <text class="confirm-text">确认</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const selectedDates = ref<Set<string>>(new Set())

const showMonthPicker = ref(false)
const pickerYear = ref(currentYear.value)
const pickerMonth = ref(currentMonth.value)

const yearOptions = computed(() => {
  const years = []
  for (let y = 2000; y <= 2099; y++) {
    years.push(y)
  }
  return years
})

const monthOptions = computed(() => {
  return [
    { value: 1, label: '1月' },
    { value: 2, label: '2月' },
    { value: 3, label: '3月' },
    { value: 4, label: '4月' },
    { value: 5, label: '5月' },
    { value: 6, label: '6月' },
    { value: 7, label: '7月' },
    { value: 8, label: '8月' },
    { value: 9, label: '9月' },
    { value: 10, label: '10月' },
    { value: 11, label: '11月' },
    { value: 12, label: '12月' },
  ]
})

const yearScrollId = computed(() => `year-${pickerYear.value}`)
const monthScrollId = computed(() => `month-${pickerMonth.value}`)

interface CalendarDate {
  year: number
  month: number
  day: number
  isCurrentMonth: boolean
}

function formatDateKey(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function getTodayKey(): string {
  const today = new Date()
  return formatDateKey(today.getFullYear(), today.getMonth() + 1, today.getDate())
}

const calendarDates = computed<CalendarDate[]>(() => {
  const dates: CalendarDate[] = []
  const daysInMonth = new Date(currentYear.value, currentMonth.value, 0).getDate()
  const firstDay = new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
  
  for (let i = 0; i < firstDay; i++) {
    dates.push({
      year: 0,
      month: 0,
      day: 0,
      isCurrentMonth: false
    })
  }

  for (let day = 1; day <= daysInMonth; day++) {
    dates.push({
      year: currentYear.value,
      month: currentMonth.value,
      day,
      isCurrentMonth: true
    })
  }

  const remainingDays = 42 - dates.length
  for (let i = 0; i < remainingDays; i++) {
    dates.push({
      year: 0,
      month: 0,
      day: 0,
      isCurrentMonth: false
    })
  }

  return dates
})

function prevYear() {
  currentYear.value--
}

function nextYear() {
  currentYear.value++
}

function prevMonth() {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

function isFutureDate(date: CalendarDate): boolean {
  if (!date.isCurrentMonth) return false
  
  const today = new Date()
  const todayYear = today.getFullYear()
  const todayMonth = today.getMonth() + 1
  const todayDay = today.getDate()

  if (date.year > todayYear) return true
  if (date.year === todayYear && date.month > todayMonth) return true
  if (date.year === todayYear && date.month === todayMonth && date.day > todayDay) return true
  return false
}

function isToday(date: CalendarDate): boolean {
  return date.isCurrentMonth && formatDateKey(date.year, date.month, date.day) === getTodayKey()
}

function isSelected(date: CalendarDate): boolean {
  return date.isCurrentMonth && selectedDates.value.has(formatDateKey(date.year, date.month, date.day))
}

function getDateClass(date: CalendarDate): string {
  const classes = ['date-cell']
  
  if (!date.isCurrentMonth) {
    classes.push('date-empty')
  } else {
    if (isFutureDate(date)) {
      classes.push('date-future')
    }
    if (isSelected(date)) {
      classes.push('date-selected')
    }
    if (isToday(date)) {
      classes.push('date-today')
    }
  }
  
  return classes.join(' ')
}

function handleDateTap(date: CalendarDate) {
  if (!date.isCurrentMonth || isFutureDate(date)) return
  
  const key = formatDateKey(date.year, date.month, date.day)
  if (selectedDates.value.has(key)) {
    selectedDates.value.delete(key)
  } else {
    selectedDates.value.add(key)
  }
  selectedDates.value = new Set(selectedDates.value)
}

function confirmMonthPicker() {
  currentYear.value = pickerYear.value
  currentMonth.value = pickerMonth.value
  showMonthPicker.value = false
}
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;
$light-pink: #FFB6C1;
$bg-color: #FFF9FA;
$card-bg: #FFFFFF;

.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, $bg-color 0%, #FFF5F7 100%);
  padding-bottom: env(safe-area-inset-bottom);
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

.header-area {
  padding: 60rpx 48rpx 40rpx;
  text-align: center;
}

.header-title-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20rpx;
  margin-bottom: 32rpx;
}

.header-icon {
  width: 80rpx;
  height: 80rpx;
}

.header-title {
  font-size: 48rpx;
  font-weight: 600;
  color: $primary-color;
}

.header-divider {
  width: 200rpx;
  height: 4rpx;
  background: linear-gradient(90deg, transparent, $light-pink, transparent);
  margin: 0 auto;
  border-radius: 2rpx;
}

.calendar-card {
  margin: 0 32rpx;
  background: $card-bg;
  border-radius: 40rpx;
  padding: 40rpx;
  box-shadow: 0 12rpx 32rpx rgba(255, 182, 193, 0.1);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40rpx;
}

.year-nav, .month-nav {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.nav-btn-lg {
  width: 96rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 182, 193, 0.3);
  border-radius: 50%;
  transition: all 0.2s;
}

.nav-btn-lg:active {
  transform: scale(0.9);
  background: rgba(255, 105, 180, 0.4);
}

.nav-icon-lg {
  width: 48rpx;
  height: 48rpx;
}

.month-select-btn {
  flex: 1;
  max-width: 480rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 28rpx 48rpx;
  background: linear-gradient(135deg, rgba(255, 105, 180, 0.1) 0%, rgba(255, 182, 193, 0.2) 100%);
  border-radius: 40rpx;
  border: 3rpx solid rgba(255, 182, 193, 0.5);
  transition: all 0.2s;
}

.month-select-btn:active {
  transform: scale(0.98);
  background: linear-gradient(135deg, rgba(255, 105, 180, 0.2) 0%, rgba(255, 182, 193, 0.3) 100%);
}

.month-text {
  font-size: 36rpx;
  font-weight: 600;
  color: $primary-color;
}

.arrow-icon {
  width: 36rpx;
  height: 36rpx;
}

.weekday-row {
  display: flex;
  margin-bottom: 24rpx;
}

.weekday {
  flex: 1;
  text-align: center;
  font-size: 28rpx;
  color: #999;
  padding: 16rpx 0;
}

.calendar-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.date-cell {
  width: calc((100% - 96rpx) / 7);
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  transition: all 0.2s;
  position: relative;
}

.date-empty {
  background: transparent;
}

.date-cell:not(.date-empty):not(.date-future):active {
  transform: scale(0.92);
}

.date-text {
  font-size: 30rpx;
  color: #333;
  font-weight: 500;
}

.date-future {
  background: rgba(200, 200, 200, 0.15);
  pointer-events: none;
}

.date-future .date-text {
  color: #BBB;
}

.date-selected {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  box-shadow: 0 8rpx 20rpx rgba(255, 105, 180, 0.35);
}

.date-selected .date-text {
  color: #FFFFFF;
  font-weight: 600;
}

.date-today:not(.date-selected) {
  background: rgba(255, 105, 180, 0.08);
}

.date-today:not(.date-selected) .date-text {
  color: $primary-color;
  font-weight: 600;
}

.date-today.date-selected {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
}

.date-today.date-selected .date-text {
  color: #FFFFFF;
}

.calendar-hint {
  margin-top: 40rpx;
  padding-top: 28rpx;
  border-top: 1rpx solid rgba(255, 182, 193, 0.3);
  text-align: center;
}

.hint-text {
  font-size: 26rpx;
  color: rgba(150, 150, 150, 0.7);
  line-height: 1.6;
}

.picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.picker-popup {
  width: 100%;
  background: $card-bg;
  border-radius: 48rpx 48rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 48rpx;
  border-bottom: 1rpx solid rgba(255, 182, 193, 0.3);
}

.picker-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $primary-color;
}

.picker-close {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-icon {
  width: 40rpx;
  height: 40rpx;
}

.picker-content {
  display: flex;
  height: 480rpx;
  padding: 24rpx 0;
}

.picker-column {
  flex: 1;
  height: 100%;
}

.picker-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
}

.picker-text {
  font-size: 32rpx;
  color: #999;
  transition: all 0.2s;
}

.picker-item-active .picker-text {
  font-size: 40rpx;
  font-weight: 600;
  color: $primary-color;
}

.picker-divider {
  width: 1rpx;
  background: rgba(255, 182, 193, 0.3);
  margin: 0 16rpx;
}

.picker-footer {
  padding: 24rpx 48rpx;
  border-top: 1rpx solid rgba(255, 182, 193, 0.3);
}

.picker-confirm {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 40rpx;
  padding: 28rpx;
  text-align: center;
  transition: all 0.2s;
}

.picker-confirm:active {
  transform: scale(0.98);
}

.confirm-text {
  font-size: 36rpx;
  font-weight: 600;
  color: #FFFFFF;
}
</style>