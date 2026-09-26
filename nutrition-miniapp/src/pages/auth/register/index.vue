<template>
  <view class="auth-page">
    <view class="brand-block">
      <view class="logo-wrap">
        <svg viewBox="0 0 80 80" class="logo-icon">
          <circle cx="40" cy="40" r="35" fill="#FF69B4"/>
          <circle cx="32" cy="38" r="5" fill="#fff"/>
          <circle cx="48" cy="38" r="5" fill="#fff"/>
          <circle cx="33" cy="37" r="2" fill="#333"/>
          <circle cx="49" cy="37" r="2" fill="#333"/>
          <path d="M40 48 Q37 52 40 56 Q43 52 40 48" stroke="#fff" stroke-width="3" fill="none"/>
        </svg>
      </view>
      <text class="app-name">创建账号</text>
      <text class="app-slogan">开启属于你的健康饮食记录</text>
    </view>

    <view class="auth-card">
      <view class="form-item">
        <text class="field-label">用户名</text>
        <input
          v-model="form.username"
          class="field-input"
          type="text"
          maxlength="32"
          placeholder="3-32位用户名"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="form-item">
        <text class="field-label">密码</text>
        <input
          v-model="form.password"
          class="field-input"
          type="password"
          maxlength="32"
          placeholder="6-32位密码"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="form-item">
        <text class="field-label">手机号</text>
        <input
          v-model="form.phone"
          class="field-input"
          type="number"
          maxlength="11"
          placeholder="请输入11位手机号"
          placeholder-class="input-placeholder"
          @input="onPhoneInput"
        />
      </view>

      <view class="form-item">
        <text class="field-label">确认密码</text>
        <input
          v-model="form.confirmPassword"
          class="field-input"
          type="password"
          maxlength="32"
          placeholder="请再次输入密码"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="form-item">
        <text class="field-label">验证码</text>
        <view class="captcha-row">
          <input
            v-model="form.captchaCode"
            class="field-input captcha-input"
            type="text"
            inputmode="numeric"
            maxlength="4"
            placeholder="4位数字"
            placeholder-class="input-placeholder"
            @input="onCaptchaInput"
          />
          <view class="captcha-box" @tap="loadCaptcha">
            <image
              v-if="captchaImage"
              class="captcha-image"
              :src="captchaImage"
              mode="aspectFit"
            />
            <text v-else class="captcha-loading">加载中</text>
          </view>
        </view>
        <text class="captcha-tip">看不清？点击图片刷新</text>
      </view>

      <view :class="['primary-btn', { disabled: isLoading }]" @tap="handleRegister">
        <text class="primary-btn-text">{{ isLoading ? '注册中...' : '注册' }}</text>
      </view>

      <view class="switch-row">
        <text class="switch-tip">已有账号？</text>
        <text class="switch-link" @tap="goLogin">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getCaptcha, register } from '@/api/auth'

const isLoading = ref(false)
const captchaImage = ref('')
const captchaId = ref('')
const form = reactive({
  username: '',
  password: '',
  phone: '',
  confirmPassword: '',
  captchaCode: '',
})

onMounted(loadCaptcha)

async function loadCaptcha() {
  try {
    const response = await getCaptcha()
    captchaId.value = response.data.captchaId
    captchaImage.value = response.data.imageBase64
    form.captchaCode = ''
  } catch (error) {
    captchaImage.value = ''
    captchaId.value = ''
    showToast(error instanceof Error ? error.message : '验证码加载失败')
  }
}

async function handleRegister() {
  if (isLoading.value) return
  const username = form.username.trim()
  const password = form.password
  const phone = form.phone.trim()
  const confirmPassword = form.confirmPassword
  const captchaCode = form.captchaCode.trim()

  if (username.length < 3 || username.length > 32) {
    showToast('用户名长度为3-32位')
    return
  }
  if (password.length < 6 || password.length > 32) {
    showToast('密码长度为6-32位')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    showToast('请输入合法的11位手机号')
    return
  }
  if (password !== confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }
  if (!/^\d{4}$/.test(captchaCode) || !captchaId.value) {
    showToast('请输入正确的4位数字验证码')
    return
  }

  isLoading.value = true
  try {
    await register({
      username,
      password,
      phone,
      confirmPassword,
      captchaId: captchaId.value,
      captchaCode,
    })
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: `/pages/auth/login/index?username=${encodeURIComponent(username)}` })
    }, 700)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '注册失败，请重试')
    await loadCaptcha()
  } finally {
    isLoading.value = false
  }
}

function goLogin() {
  uni.reLaunch({ url: '/pages/auth/login/index' })
}

function onCaptchaInput(event: { detail: { value: string } }) {
  form.captchaCode = String(event.detail.value || '').replace(/\D/g, '').slice(0, 4)
}

function onPhoneInput(event: { detail: { value: string } }) {
  form.phone = String(event.detail.value || '').replace(/\D/g, '').slice(0, 11)
}

function showToast(title: string) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;

.auth-page {
  min-height: 100vh;
  padding: 58rpx 44rpx 44rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(180deg, #FFF9FA 0%, #FFF2F6 100%);
  position: relative;
  overflow: hidden;
}

.auth-page::before {
  content: '';
  position: fixed;
  top: -160rpx;
  right: -140rpx;
  width: 420rpx;
  height: 420rpx;
  border-radius: 50%;
  background: rgba(255, 182, 193, 0.22);
}

.brand-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 36rpx;
  position: relative;
  z-index: 1;
}

.logo-wrap {
  width: 118rpx;
  height: 118rpx;
  border-radius: 38rpx;
  background: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 18rpx 40rpx rgba(255, 105, 180, 0.18);
  margin-bottom: 18rpx;
}

.logo-icon {
  width: 86rpx;
  height: 86rpx;
}

.app-name {
  font-size: 43rpx;
  font-weight: 700;
  color: #3D2932;
}

.app-slogan {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #A58A95;
}

.auth-card {
  width: 100%;
  padding: 40rpx 36rpx 36rpx;
  border-radius: 40rpx;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20rpx 60rpx rgba(197, 120, 154, 0.14);
  position: relative;
  z-index: 1;
}

.form-item {
  margin-bottom: 25rpx;
}

.field-label {
  display: block;
  margin-bottom: 11rpx;
  font-size: 27rpx;
  font-weight: 600;
  color: #634752;
}

.field-input {
  width: 100%;
  height: 86rpx;
  padding: 0 27rpx;
  box-sizing: border-box;
  border-radius: 23rpx;
  background: #FFF7FA;
  border: 1rpx solid rgba(255, 105, 180, 0.16);
  color: #3D2932;
  font-size: 28rpx;
}

.input-placeholder {
  color: #C9B5BD;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-box {
  width: 210rpx;
  height: 86rpx;
  border-radius: 23rpx;
  overflow: hidden;
  background: #FFF0F5;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid rgba(255, 105, 180, 0.18);
}

.captcha-image {
  width: 100%;
  height: 100%;
}

.captcha-loading {
  font-size: 24rpx;
  color: #B895A3;
}

.captcha-tip {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #B895A3;
}

.primary-btn {
  height: 92rpx;
  margin-top: 38rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-color 0%, #FF8DC2 100%);
  box-shadow: 0 14rpx 28rpx rgba(255, 105, 180, 0.24);
}

.primary-btn:active {
  transform: scale(0.985);
}

.primary-btn.disabled {
  opacity: 0.65;
}

.primary-btn-text {
  color: #FFFFFF;
  font-size: 32rpx;
  font-weight: 700;
}

.switch-row {
  margin-top: 27rpx;
  display: flex;
  justify-content: center;
  align-items: center;
}

.switch-tip {
  font-size: 26rpx;
  color: #9B7D89;
}

.switch-link {
  margin-left: 8rpx;
  font-size: 27rpx;
  font-weight: 700;
  color: $primary-color;
}
</style>
