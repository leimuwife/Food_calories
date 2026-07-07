<template>
  <view class="page-container">
    <!-- 用户信息 -->
    <view class="profile-header">
      <image class="avatar" :src="getAvatarUrl(userStore.userInfo?.fileIds)" mode="aspectFill" />
      <view class="profile-info">
        <text class="profile-name">{{ userStore.userInfo?.nickname || '未登录' }}</text>
        <text class="profile-email">{{ userStore.userInfo?.email || '点击登录体验完整功能' }}</text>
      </view>
      <u-icon name="arrow-right" size="16" color="#C0C4CC" />
    </view>

    <!-- 登录/注册按钮（未登录时） -->
    <view class="card" v-if="!userStore.isLoggedIn">
      <u-button
        type="primary"
        :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx' }"
        text="微信一键登录"
        open-type="getPhoneNumber"
        @tap="handleWxLogin"
      />
      <u-button
        :custom-style="{ marginTop: '16rpx', borderRadius: '40rpx' }"
        text="账号密码登录"
        @tap="showLoginDialog = true"
      />
    </view>

    <!-- 营养目标设置 -->
    <view class="card">
      <text class="card-title">每日营养目标</text>
      <view class="goal-grid">
        <view class="goal-item" @tap="editGoal('calorieGoal', '热量目标(kcal)', userStore.userInfo?.dailyCalorieGoal || 2000)">
          <text class="goal-value">{{ userStore.userInfo?.dailyCalorieGoal || 2000 }}</text>
          <text class="goal-label">热量(kcal)</text>
        </view>
        <view class="goal-item" @tap="editGoal('proteinGoal', '蛋白质目标(g)', userStore.userInfo?.dailyProteinGoal || 60)">
          <text class="goal-value protein-color">{{ userStore.userInfo?.dailyProteinGoal || 60 }}</text>
          <text class="goal-label">蛋白质(g)</text>
        </view>
        <view class="goal-item" @tap="editGoal('fatGoal', '脂肪目标(g)', userStore.userInfo?.dailyFatGoal || 55)">
          <text class="goal-value fat-color">{{ userStore.userInfo?.dailyFatGoal || 55 }}</text>
          <text class="goal-label">脂肪(g)</text>
        </view>
        <view class="goal-item" @tap="editGoal('carbsGoal', '碳水目标(g)', userStore.userInfo?.dailyCarbsGoal || 250)">
          <text class="goal-value carbs-color">{{ userStore.userInfo?.dailyCarbsGoal || 250 }}</text>
          <text class="goal-label">碳水(g)</text>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="card menu-list">
      <view class="menu-item" @tap="handleExport">
        <text>数据导出 (CSV)</text>
        <u-icon name="arrow-right" size="14" color="#C0C4CC" />
      </view>
      <view class="menu-item" @tap="showDisclaimer = true">
        <text>免责声明</text>
        <u-icon name="arrow-right" size="14" color="#C0C4CC" />
      </view>
      <view class="menu-item" @tap="showAbout = true">
        <text>关于我们</text>
        <u-icon name="arrow-right" size="14" color="#C0C4CC" />
      </view>
      <view class="menu-item" v-if="userStore.isLoggedIn" @tap="handleLogout">
        <text class="logout-text">退出登录</text>
        <u-icon name="arrow-right" size="14" color="#C0C4CC" />
      </view>
    </view>

    <!-- 登录弹窗 -->
    <u-popup :show="showLoginDialog" mode="center" :round="20" @close="showLoginDialog = false">
      <view class="login-dialog">
        <text class="login-title">账号登录</text>
        <u-input v-model="loginForm.username" placeholder="用户名" :custom-style="{ marginBottom: '16rpx' }" />
        <u-input v-model="loginForm.password" type="password" placeholder="密码" :custom-style="{ marginBottom: '24rpx' }" />
        <u-button
          type="primary"
          :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx' }"
          text="登录"
          @tap="handleLogin"
        />
        <text class="register-link" @tap="showRegisterDialog = true; showLoginDialog = false">没有账号？去注册</text>
      </view>
    </u-popup>

    <!-- 注册弹窗 -->
    <u-popup :show="showRegisterDialog" mode="center" :round="20" @close="showRegisterDialog = false">
      <view class="login-dialog">
        <text class="login-title">账号注册</text>
        <u-input v-model="registerForm.username" placeholder="用户名" :custom-style="{ marginBottom: '16rpx' }" />
        <u-input v-model="registerForm.nickname" placeholder="昵称" :custom-style="{ marginBottom: '16rpx' }" />
        <u-input v-model="registerForm.password" type="password" placeholder="密码" :custom-style="{ marginBottom: '24rpx' }" />
        <u-button
          type="primary"
          :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx' }"
          text="注册"
          @tap="handleRegister"
        />
        <text class="register-link" @tap="showLoginDialog = true; showRegisterDialog = false">已有账号？去登录</text>
      </view>
    </u-popup>

    <!-- 目标编辑弹窗 -->
    <u-popup :show="showGoalDialog" mode="center" :round="20" @close="showGoalDialog = false">
      <view class="login-dialog">
        <text class="login-title">{{ goalFieldLabel }}</text>
        <u-input v-model="goalValue" type="number" :custom-style="{ marginBottom: '24rpx' }" />
        <u-button
          type="primary"
          :custom-style="{ backgroundColor: '#7EC8A0', borderColor: '#7EC8A0', borderRadius: '40rpx' }"
          text="保存"
          @tap="saveGoal"
        />
      </view>
    </u-popup>

    <!-- 免责声明弹窗 -->
    <u-popup :show="showDisclaimer" mode="center" :round="20" @close="showDisclaimer = false">
      <view class="disclaimer-dialog">
        <text class="disclaimer-title">免责声明</text>
        <text class="disclaimer-content">本工具提供的营养数据仅供参考，不构成医疗或膳食建议。食物营养数据来源于公开数据库，可能与实际存在偏差。如需专业的饮食指导，请咨询注册营养师或医生。用户因使用本工具而产生的任何后果，开发者不承担法律责任。</text>
        <u-button text="我知道了" :custom-style="{ borderRadius: '40rpx', marginTop: '24rpx' }" @tap="showDisclaimer = false" />
      </view>
    </u-popup>

    <!-- 关于弹窗 -->
    <u-popup :show="showAbout" mode="center" :round="20" @close="showAbout = false">
      <view class="disclaimer-dialog">
        <text class="disclaimer-title">关于营养助手</text>
        <text class="disclaimer-content">营养助手 v1.0.0\n一款专注于食物热量与营养管理的微信小程序。帮助您科学管理每日饮食，实现健康生活目标。</text>
        <u-button text="知道了" :custom-style="{ borderRadius: '40rpx', marginTop: '24rpx' }" @tap="showAbout = false" />
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { accountLogin, register, exportData } from '@/api'
import { updateNutritionGoal } from '@/api/wode/wode'
import type { RegisterParam, NutritionGoalUpdateParam } from '@/api/types'
import { getToday, formatDate } from '@/utils'

const userStore = useUserStore()

function getAvatarUrl(fileIds: string | null | undefined): string {
  if (!fileIds) {
    return '/static/images/default-avatar.png'
  }
  let firstId: string | null = null
  try {
    const ids = JSON.parse(fileIds)
    if (Array.isArray(ids) && ids.length > 0) {
      firstId = String(ids[0])
    }
  } catch {
    const parts = fileIds.split(',')
    if (parts.length > 0) {
      firstId = parts[0].trim()
    }
  }
  return firstId ? `/api/attachment/${firstId}/url` : '/static/images/default-avatar.png'
}

// 登录
const showLoginDialog = ref(false)
const loginForm = reactive({ username: '', password: '' })

// 注册
const showRegisterDialog = ref(false)
const registerForm = reactive<RegisterParam>({ username: '', nickname: '', password: '' })

// 目标编辑
const showGoalDialog = ref(false)
const goalFieldLabel = ref('')
const goalField = ref('')
const goalValue = ref('')

// 免责声明 & 关于
const showDisclaimer = ref(false)
const showAbout = ref(false)

async function handleWxLogin() {
  uni.navigateTo({ url: '/pages/weChatLogin/index' })
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  try {
    const res = await accountLogin(loginForm.username, loginForm.password)
    userStore.setLogin(res.data.token, res.data.user)
    uni.showToast({ title: '登录成功', icon: 'success' })
    showLoginDialog.value = false
  } catch (e) { /* handled in api */ }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  try {
    const res = await register(registerForm)
    userStore.setLogin(res.data.token, res.data.user)
    uni.showToast({ title: '注册成功', icon: 'success' })
    showRegisterDialog.value = false
  } catch (e) { /* handled in api */ }
}

function editGoal(field: string, label: string, currentValue: number) {
  goalField.value = field
  goalFieldLabel.value = label
  goalValue.value = String(currentValue)
  showGoalDialog.value = true
}

async function saveGoal() {
  try {
    await updateNutritionGoal({ [goalField.value]: Number(goalValue.value) })
    userStore.updateUser({ [goalField.value]: Number(goalValue.value) } as any)
    uni.showToast({ title: '保存成功', icon: 'success' })
    showGoalDialog.value = false
  } catch (e) { /* handled */ }
}

async function handleExport() {
  try {
    const today = getToday()
    const res = await exportData(today, today)
    const csv = (res.data as any)?.csvContent || ''
    uni.showToast({ title: '导出功能需在开发工具中测试', icon: 'none' })
  } catch (e) { /* handled */ }
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.showToast({ title: '已退出', icon: 'success' })
      }
    },
  })
}
</script>

<style lang="scss" scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 32rpx 24rpx;
  background: linear-gradient(135deg, #7EC8A0 0%, #5BA07A 100%);
  margin-bottom: 16rpx;
}
.avatar { width: 100rpx; height: 100rpx; border-radius: 50%; border: 3rpx solid #FFFFFF; }
.profile-info { flex: 1; }
.profile-name { font-size: 34rpx; font-weight: 600; color: #FFFFFF; display: block; }
.profile-email { font-size: 24rpx; color: rgba(255,255,255,0.8); margin-top: 4rpx; display: block; }
.goal-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx; margin-top: 8rpx; }
.goal-item { background: #F5F7FA; border-radius: 16rpx; padding: 24rpx; text-align: center; }
.goal-value { font-size: 40rpx; font-weight: 700; color: #7EC8A0; display: block; }
.protein-color { color: #FF6B6B; }
.fat-color { color: #FFA94D; }
.carbs-color { color: #4ECDC4; }
.goal-label { font-size: 22rpx; color: #909399; margin-top: 4rpx; display: block; }
.menu-list { margin-top: 16rpx; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #F0F0F0; font-size: 28rpx; color: #303133; }
.menu-item:last-child { border-bottom: none; }
.logout-text { color: #F56C6C; }
.login-dialog { padding: 48rpx 32rpx 40rpx; width: 560rpx; }
.login-title { font-size: 34rpx; font-weight: 600; color: #303133; display: block; text-align: center; margin-bottom: 32rpx; }
.register-link { font-size: 26rpx; color: #7EC8A0; display: block; text-align: center; margin-top: 20rpx; }
.disclaimer-dialog { padding: 40rpx 32rpx 32rpx; width: 560rpx; }
.disclaimer-title { font-size: 34rpx; font-weight: 600; color: #303133; display: block; text-align: center; margin-bottom: 20rpx; }
.disclaimer-content { font-size: 26rpx; color: #606266; line-height: 1.8; }
</style>
