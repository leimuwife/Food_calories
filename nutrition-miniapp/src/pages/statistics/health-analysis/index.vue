<template>
  <view class="page-container">
    <view class="page-header">
      <view class="header-copy">
        <text class="page-title">健康分析</text>
        <text class="page-subtitle">用每日热量趋势，找到更适合你的饮食节奏</text>
      </view>
      <view class="header-icon-wrap">
        <svg viewBox="0 0 80 80" class="header-icon">
          <rect x="12" y="48" width="10" height="18" rx="4" fill="#FFB6C1"/>
          <rect x="28" y="30" width="10" height="36" rx="4" fill="#FF69B4"/>
          <rect x="44" y="20" width="10" height="46" rx="4" fill="#FF8DC2"/>
          <rect x="60" y="40" width="10" height="26" rx="4" fill="#FFB6C1"/>
          <path d="M14 28 Q32 10 52 18 Q64 22 70 12" stroke="#FF69B4" stroke-width="3" fill="none" stroke-linecap="round"/>
        </svg>
      </view>
    </view>

    <view class="charts-responsive">
      <CalorieBarChart title="最近一周热量" :data="calorieTrend.last7Days" />
      <CalorieBarChart title="最近30天热量" :data="calorieTrend.last30Days" scrollable />
    </view>

    <view class="report-card">
      <view class="report-header">
        <view>
          <text class="report-title">AI 智能分析报告</text>
          <text class="report-subtitle">选择目标后，AI 会结合历史热量给出饮食建议</text>
        </view>
        <view class="ai-badge">AI</view>
      </view>

      <scroll-view scroll-x class="goal-scroll" :show-scrollbar="false">
        <view class="goal-list">
          <view
            v-for="goal in goalOptions"
            :key="goal.value"
            :class="['goal-item', { active: selectedGoal === goal.value }]"
            @tap="selectGoal(goal.value)"
          >
            {{ goal.label }}
          </view>
        </view>
      </scroll-view>

      <view :class="['generate-btn', { disabled: isGenerating }]" @tap="handleGenerate">
        <text class="generate-text">{{ isGenerating ? 'AI 分析中，请稍候...' : '生成分析报告' }}</text>
      </view>

      <view v-if="report?.reportStatus === 'fallback'" class="fallback-tip">
        AI 服务暂时不可用，当前展示的是同目标历史报告。
      </view>

      <view v-if="isReportLoading" class="report-status">报告加载中...</view>
      <view v-else-if="report" class="report-body">
        <view class="report-meta">
          <text class="report-goal">{{ report.goalLabel }}</text>
          <text class="report-time">{{ formatTime(report.generatedTime) }}</text>
        </view>
        <text class="report-content">{{ report.reportContent }}</text>
        <view class="average-row">
          <view class="average-item">
            <text class="average-value">{{ formatAverage(report.last7Avg) }}</text>
            <text class="average-label">近7天日均 kcal</text>
          </view>
          <view class="average-item">
            <text class="average-value">{{ formatAverage(report.last30Avg) }}</text>
            <text class="average-label">近30天日均 kcal</text>
          </view>
        </view>
      </view>
      <view v-else class="report-status">该目标暂无历史报告，点击上方按钮生成。</view>
    </view>

    <text class="disclaimer">分析结果仅供参考，不构成医疗建议</text>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import CalorieBarChart from '@/components/statistics/CalorieBarChart.vue'
import {
  generateHealthReport,
  getHealthCalorieTrend,
  getLatestHealthReport,
} from '@/api/statistics/healthAnalysis'
import type { HealthAnalysisReportVO, HealthCalorieTrendVO, HealthGoalType } from '@/api/types'

const goalOptions: Array<{ value: HealthGoalType; label: string }> = [
  { value: 'fitness', label: '健身' },
  { value: 'weight_loss', label: '减肥' },
  { value: 'normal_diet', label: '正常饮食' },
  { value: 'weight_gain', label: '增重' },
]

const calorieTrend = reactive<HealthCalorieTrendVO>({
  last7Days: [],
  last30Days: [],
})
const selectedGoal = ref<HealthGoalType>('normal_diet')
const report = ref<HealthAnalysisReportVO | null>(null)
const isReportLoading = ref(false)
const isGenerating = ref(false)

onMounted(async () => {
  await Promise.all([loadCalorieTrend(), loadLatestReport()])
})

async function loadCalorieTrend() {
  try {
    const response = await getHealthCalorieTrend()
    calorieTrend.last7Days = response.data.last7Days || []
    calorieTrend.last30Days = response.data.last30Days || []
  } catch (error) {
    showToast(error instanceof Error ? error.message : '热量数据加载失败')
  }
}

async function loadLatestReport() {
  const goal = selectedGoal.value
  isReportLoading.value = true
  try {
    const response = await getLatestHealthReport(goal)
    if (selectedGoal.value === goal) {
      report.value = response.data
    }
  } catch (error) {
    if (selectedGoal.value === goal) {
      report.value = null
      showToast(error instanceof Error ? error.message : '历史报告加载失败')
    }
  } finally {
    if (selectedGoal.value === goal) {
      isReportLoading.value = false
    }
  }
}

async function selectGoal(goal: HealthGoalType) {
  if (isGenerating.value || selectedGoal.value === goal) return
  selectedGoal.value = goal
  report.value = null
  await loadLatestReport()
}

async function handleGenerate() {
  if (isGenerating.value) return
  isGenerating.value = true
  try {
    const response = await generateHealthReport(selectedGoal.value)
    report.value = response.data
    if (response.data.reportStatus === 'fallback') {
      showToast('AI服务暂不可用，已展示历史报告')
    } else {
      showToast('分析报告已生成')
    }
  } catch (error) {
    showToast(error instanceof Error ? error.message : '报告生成失败，请稍后再试')
  } finally {
    isGenerating.value = false
  }
}

function formatAverage(value: number | string): string {
  return String(Math.round(Number(value) || 0))
}

function formatTime(value: string): string {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function showToast(title: string) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;
$light-pink: #FFB6C1;

.page-container {
  min-height: 100vh;
  padding: 28rpx 24rpx 60rpx;
  box-sizing: border-box;
  background: linear-gradient(180deg, #FFF9FA 0%, #FFF2F6 100%);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
  padding: 30rpx 30rpx 28rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, #FFFFFF 0%, #FFF4F8 100%);
  box-shadow: 0 10rpx 30rpx rgba(255, 105, 180, 0.1);
  border: 1rpx solid rgba(255, 182, 193, 0.28);
}

.header-copy {
  flex: 1;
  min-width: 0;
}

.page-title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
  color: #4B3340;
}

.page-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #A58A95;
}

.header-icon-wrap {
  width: 116rpx;
  height: 116rpx;
  margin-left: 20rpx;
  border-radius: 34rpx;
  background: #FFF0F5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon {
  width: 90rpx;
  height: 90rpx;
}

.charts-responsive {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.report-card {
  padding: 30rpx 26rpx 30rpx;
  border-radius: 34rpx;
  background: #FFFFFF;
  box-shadow: 0 10rpx 30rpx rgba(255, 105, 180, 0.1);
  border: 1rpx solid rgba(255, 182, 193, 0.28);
}

.report-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.report-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #4B3340;
}

.report-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #A58A95;
}

.ai-badge {
  width: 58rpx;
  height: 58rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, $primary-color 0%, #FF91C5 100%);
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 16rpx rgba(255, 105, 180, 0.22);
}

.goal-scroll {
  width: 100%;
  margin-top: 28rpx;
}

.goal-list {
  display: inline-flex;
  gap: 14rpx;
}

.goal-item {
  padding: 15rpx 26rpx;
  border-radius: 28rpx;
  background: #FFF4F8;
  color: #8C6877;
  font-size: 25rpx;
  white-space: nowrap;
}

.goal-item.active {
  background: linear-gradient(135deg, $primary-color 0%, #FF8DC2 100%);
  color: #FFFFFF;
  font-weight: 700;
  box-shadow: 0 8rpx 18rpx rgba(255, 105, 180, 0.2);
}

.generate-btn {
  height: 88rpx;
  margin-top: 26rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #FF8DC2 0%, $primary-color 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 24rpx rgba(255, 105, 180, 0.22);
}

.generate-btn.disabled {
  opacity: 0.62;
}

.generate-text {
  color: #FFFFFF;
  font-size: 29rpx;
  font-weight: 700;
}

.fallback-tip {
  margin-top: 20rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #FFF7E8;
  color: #B66B16;
  font-size: 23rpx;
  line-height: 1.5;
}

.report-status {
  padding: 54rpx 10rpx 34rpx;
  text-align: center;
  color: #B895A3;
  font-size: 25rpx;
}

.report-body {
  padding-top: 26rpx;
}

.report-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.report-goal {
  padding: 8rpx 18rpx;
  border-radius: 18rpx;
  background: #FFF0F5;
  color: $primary-color;
  font-size: 23rpx;
  font-weight: 700;
}

.report-time {
  font-size: 21rpx;
  color: #B895A3;
}

.report-content {
  display: block;
  white-space: pre-wrap;
  color: #5F4652;
  font-size: 27rpx;
  line-height: 1.75;
}

.average-row {
  display: flex;
  gap: 18rpx;
  margin-top: 28rpx;
}

.average-item {
  flex: 1;
  padding: 20rpx 14rpx;
  border-radius: 22rpx;
  background: #FFF7FA;
  text-align: center;
}

.average-value {
  display: block;
  color: $primary-color;
  font-size: 32rpx;
  font-weight: 700;
}

.average-label {
  display: block;
  margin-top: 6rpx;
  color: #A58A95;
  font-size: 20rpx;
}

.disclaimer {
  display: block;
  margin-top: 30rpx;
  text-align: center;
  color: #B79DA7;
  font-size: 22rpx;
}

@media (max-width: 520px) {
  .charts-responsive {
    display: block;
  }

  .charts-responsive > * {
    margin-bottom: 20rpx;
  }
}
</style>
