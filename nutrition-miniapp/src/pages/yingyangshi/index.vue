<template>
  <view class="page-container">
    <view class="header-area">
      <view class="header-nav">
        <view class="nav-icon-btn" @tap="openHistoryDrawer">
          <svg viewBox="0 0 48 48" class="nav-icon">
            <path d="M12 15 L36 15 M12 24 L36 24 M12 33 L36 33" stroke="#FF69B4" stroke-width="4" stroke-linecap="round"/>
          </svg>
        </view>
        <view class="header-title-wrap">
          <image src="/static/AI/nutritionist.png" class="header-avatar" mode="aspectFill"/>
          <view>
            <text class="nutritionist-name">小张营养师</text>
            <text class="nutritionist-desc">专业营养咨询</text>
          </view>
        </view>
        <view class="nav-icon-btn" @tap="startNewChat">
          <svg viewBox="0 0 48 48" class="nav-icon">
            <path d="M24 12 L24 36 M12 24 L36 24" stroke="#FF69B4" stroke-width="4" stroke-linecap="round"/>
          </svg>
        </view>
      </view>
    </view>

    <view v-if="showHistoryDrawer" class="drawer-mask" @tap="closeHistoryDrawer">
      <view class="history-drawer" @tap.stop>
        <view class="drawer-header">
          <view>
            <text class="drawer-title">历史对话</text>
            <text class="drawer-subtitle">继续之前的营养咨询</text>
          </view>
          <view class="drawer-close" @tap="closeHistoryDrawer">
            <svg viewBox="0 0 48 48" class="drawer-close-icon">
              <path d="M17 17 L31 31 M31 17 L17 31" stroke="#B895A3" stroke-width="3" stroke-linecap="round"/>
            </svg>
          </view>
        </view>

        <scroll-view scroll-y class="history-scroll">
          <view v-if="isHistoryLoading" class="history-status">加载中...</view>
          <view v-else-if="chatSessions.length === 0" class="history-status">暂无历史对话</view>
          <template v-else>
            <view
              v-for="session in chatSessions"
              :key="session.sessionId"
              :class="['history-item', { active: currentSessionId === String(session.sessionId) }]"
              @tap="selectSession(session)"
            >
              <view class="history-icon">
                <svg viewBox="0 0 48 48" class="history-icon-svg">
                  <path d="M12 13 Q12 9 16 9 L32 9 Q36 9 36 13 L36 29 Q36 33 32 33 L22 33 L15 39 L15 33 Q12 33 12 29 Z" fill="#FFB6C1"/>
                  <path d="M18 18 L30 18 M18 24 L27 24" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
              </view>
              <view class="history-info">
                <text class="history-title">{{ session.lastMessage || '新对话' }}</text>
                <text class="history-time">{{ formatSessionTime(session.updateTime || session.createTime) }}</text>
              </view>
            </view>
          </template>
        </scroll-view>

        <view class="new-chat-footer" @tap="startNewChat">
          <text class="new-chat-plus">＋</text>
          <text class="new-chat-text">新建对话</text>
        </view>
      </view>
    </view>

    <scroll-view 
      scroll-y 
      class="chat-scroll"
      :scroll-into-view="scrollToId"
      :scroll-with-animation="true"
    >
      <view v-for="msg in chatMessages" :key="msg.id" :id="'msg-' + msg.id" class="message-item">
        <view :class="['message-content', { 'message-user': msg.role === 'user', 'message-ai': msg.role === 'assistant' }]">
          <image 
            v-if="msg.role === 'assistant'" 
            src="/static/AI/nutritionist.png" 
            class="msg-avatar" 
            mode="aspectFill"
          />
          <view class="msg-bubble" @longpress="copyMessage(msg.content)">
            <text class="msg-text">{{ msg.content }}</text>
            <view v-if="msg.images && msg.images.length > 0" class="msg-images">
              <image 
                v-for="(img, idx) in msg.images" 
                :key="idx" 
                :src="img" 
                class="msg-image" 
                mode="aspectFill"
                @tap="previewImage(msg.images, idx)"
              />
            </view>
          </view>
          <image 
            v-if="msg.role === 'user'" 
            :src="userAvatar" 
            class="msg-avatar" 
            mode="aspectFill"
          />
        </view>
      </view>

      <view v-if="isLoading" class="loading-item">
        <view class="loading-content">
          <image src="/static/AI/nutritionist.png" class="loading-avatar" mode="aspectFill"/>
          <view class="loading-dots">
            <view class="dot"></view>
            <view class="dot"></view>
            <view class="dot"></view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="disclaimer">
      <text class="disclaimer-text">⚠️ 答案由AI生成，仅供参考，不构成医疗建议</text>
    </view>

    <view class="input-area">
      <view class="image-preview">
        <view 
          v-for="(img, idx) in selectedImages" 
          :key="idx" 
          class="preview-item"
        >
          <image :src="img.url" class="preview-image" mode="aspectFill"/>
          <view class="preview-delete" @tap="removeImage(idx)">
            <svg viewBox="0 0 48 48" class="delete-icon">
              <circle cx="24" cy="24" r="18" fill="#FF69B4"/>
              <path d="M18 18 L30 30 M30 18 L18 30" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round"/>
            </svg>
          </view>
        </view>
      </view>

      <view class="input-row">
        <view class="image-btn" @tap="chooseImage">
          <svg viewBox="0 0 48 48" class="image-icon">
            <rect x="8" y="8" width="32" height="32" rx="8" fill="#FF69B4"/>
            <circle cx="20" cy="18" r="4" fill="#fff"/>
            <circle cx="32" cy="24" r="3" fill="#fff"/>
            <rect x="12" y="28" width="8" height="4" rx="1" fill="#fff"/>
          </svg>
        </view>

        <textarea 
          v-model="inputContent" 
          class="text-input"
          placeholder="输入你的饮食、减肥相关问题咨询小张营养师"
          :maxlength="500"
          :auto-height="true"
          :adjust-position="true"
          @input="onInput"
        />

        <view :class="['send-btn', { 'send-btn-disabled': !canSend }]" @tap="sendMessage">
          <text class="send-text">发送</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getChatHistory, getChatSessions, nutritionistChat } from '@/api/yingyangshi/yingyangshi'
import { uploadAttachment, getAttachmentUrl } from '@/api'
import type { ChatMessage, ChatSessionVO } from '@/api/types'

const userStore = useUserStore()
const chatMessages = ref<ChatMessage[]>([])
const inputContent = ref('')
const selectedImages = ref<{ fileId: string; url: string }[]>([])
const isLoading = ref(false)
const scrollToId = ref('')
// 当前会话ID：首次对话为空，后端新建会话后回传并保存；重新进入页面为空即开启新对话
const currentSessionId = ref('')
const chatSessions = ref<ChatSessionVO[]>([])
const showHistoryDrawer = ref(false)
const isHistoryLoading = ref(false)

interface SelectedImage {
  fileId: string
  url: string
}

const canSend = computed(() => {
  return inputContent.value.trim().length > 0 || selectedImages.value.length > 0
})

const userAvatar = computed(() => {
  if (!userStore.userInfo?.fileIds) {
    return getDefaultAvatar()
  }
  let firstId: string | null = null
  try {
    const ids = JSON.parse(userStore.userInfo.fileIds)
    if (Array.isArray(ids) && ids.length > 0) {
      firstId = String(ids[0])
    }
  } catch {
    const parts = userStore.userInfo.fileIds.split(',')
    if (parts.length > 0) {
      firstId = parts[0].trim()
    }
  }
  return firstId ? getAttachmentUrl(String(firstId)) : getDefaultAvatar()
})

function getDefaultAvatar(): string {
  return 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"%3E%3Ccircle cx="50" cy="50" r="45" fill="%23FFB6C1"/%3E%3Ccircle cx="38" cy="42" r="6" fill="%23333"/%3E%3Ccircle cx="62" cy="42" r="6" fill="%23333"/%3E%3Ccircle cx="39" cy="41" r="2" fill="%23fff"/%3E%3Ccircle cx="63" cy="41" r="2" fill="%23fff"/%3E%3Cellipse cx="50" cy="56" rx="5" ry="3" fill="%23FF69B4"/%3E%3Cpath d="M42 64 Q50 70 58 64" stroke="%23333" stroke-width="2" fill="none"/%3E%3C/svg%3E'
}

function onInput() {}

function openHistoryDrawer() {
  showHistoryDrawer.value = true
  loadSessionList()
}

function closeHistoryDrawer() {
  showHistoryDrawer.value = false
}

function createWelcomeMessage(): ChatMessage {
  return {
    id: Date.now(),
    role: 'assistant',
    content: '您好！我是小张营养师，很高兴为您服务～\n\n您可以向我咨询：\n🍎 饮食搭配建议\n🏃 减肥计划\n🥗 营养餐单\n💡 健康小贴士\n\n也可以上传食物图片让我帮您分析热量哦！',
    createTime: new Date().toISOString()
  }
}

function startNewChat() {
  currentSessionId.value = ''
  chatMessages.value = [createWelcomeMessage()]
  inputContent.value = ''
  selectedImages.value = []
  closeHistoryDrawer()
  scrollToBottom()
}

async function loadSessionList() {
  if (!userStore.isLoggedIn) return
  isHistoryLoading.value = true
  try {
    const response = await getChatSessions()
    chatSessions.value = response.data || []
  } catch (error) {
    console.error('加载历史会话失败:', error)
  } finally {
    isHistoryLoading.value = false
  }
}

async function selectSession(session: ChatSessionVO) {
  const sessionId = String(session.sessionId)
  if (currentSessionId.value === sessionId && chatMessages.value.length > 1) {
    closeHistoryDrawer()
    return
  }

  isHistoryLoading.value = true
  try {
    const response = await getChatHistory(sessionId)
    currentSessionId.value = sessionId
    chatMessages.value = (response.data || []).map((message, index) => ({
      id: Date.now() + index,
      role: message.role === 'user' ? 'user' : 'assistant',
      content: message.content,
      createTime: message.createTime,
    }))
    if (chatMessages.value.length === 0) {
      chatMessages.value = [createWelcomeMessage()]
    }
    closeHistoryDrawer()
    await scrollToBottom()
  } catch (error) {
    uni.showToast({ title: '历史消息加载失败', icon: 'none' })
  } finally {
    isHistoryLoading.value = false
  }
}

function formatSessionTime(value: string): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const isToday = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
  if (isToday) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${date.getMonth() + 1}-${date.getDate()}`
}

function previewImage(images: string[], index: number) {
  uni.previewImage({
    urls: images,
    current: images[index]
  })
}

async function chooseImage() {
  uni.chooseImage({
    count: 9 - selectedImages.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePaths = res.tempFilePaths
      if (tempFilePaths.length === 0) return

      try {
        for (const filePath of tempFilePaths) {
          const result: any = await uploadAttachment(filePath, 'chat/')
          if (result && result.id) {
            selectedImages.value.push({
              fileId: String(result.id),
              url: filePath
            })
          }
        }
      } catch (e) {
        console.error('图片上传失败:', e)
        uni.showToast({ title: '部分图片上传失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.showToast({ title: '选择图片失败', icon: 'none' })
    }
  })
}

function removeImage(index: number) {
  selectedImages.value.splice(index, 1)
}

function copyMessage(content: string) {
  uni.setClipboardData({
    data: content,
    success: () => {
      uni.showToast({
        title: '已复制',
        icon: 'success'
      })
    },
    fail: () => {
      uni.showToast({
        title: '复制失败',
        icon: 'none'
      })
    }
  })
}

async function sendMessage() {
  if (!canSend.value) return
  if (isLoading.value) return

  const content = inputContent.value.trim()
  const fileIds = selectedImages.value.map(img => img.fileId)
  const imageUrls = selectedImages.value.map(img => img.url)

  const userMsg: ChatMessage = {
    id: Date.now(),
    role: 'user',
    content: content,
    images: imageUrls.length > 0 ? imageUrls : undefined,
    fileIds: fileIds.length > 0 ? fileIds : undefined,
    createTime: new Date().toISOString()
  }

  chatMessages.value.push(userMsg)

  inputContent.value = ''
  selectedImages.value = []

  await scrollToBottom()

  isLoading.value = true

  try {
    const res = await nutritionistChat({
      content: content,
      fileIds: fileIds.length > 0 ? fileIds : undefined,
      // 携带当前会话ID保持多轮上下文；为空时后端新建会话
      sessionId: currentSessionId.value || undefined
    })

    // 首次对话后端生成会话ID并回传，保存后后续消息携带，实现多轮记忆
    if (res.data.sessionId) {
      currentSessionId.value = res.data.sessionId
    }

    const aiMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: res.data.response || '',
        createTime: new Date().toISOString()
      }

    chatMessages.value.push(aiMsg)
    await loadSessionList()
  } catch (e) {
    console.error('聊天请求失败:', e)

    const errorMsg: ChatMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，我现在有点忙，请稍后再试～',
      createTime: new Date().toISOString()
    }

    chatMessages.value.push(errorMsg)
  } finally {
    isLoading.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (chatMessages.value.length > 0) {
    const lastMsg = chatMessages.value[chatMessages.value.length - 1]
    scrollToId.value = 'msg-' + lastMsg.id
  }
}

onMounted(async () => {
  startNewChat()
  await loadSessionList()
})
</script>

<style lang="scss" scoped>
$primary-color: #FF69B4;
$light-pink: #FFB6C1;
$bg-color: #FFF9FA;
$card-bg: #FFFFFF;

.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, $bg-color 0%, #FFF5F7 100%);
  display: flex;
  flex-direction: column;
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
  background: $card-bg;
  padding: 68rpx 28rpx 22rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-icon-btn {
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: #FFF3F7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon-btn:active {
  transform: scale(0.95);
}

.nav-icon {
  width: 42rpx;
  height: 42rpx;
}

.header-title-wrap {
  flex: 1;
  margin: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-avatar {
  width: 70rpx;
  height: 70rpx;
  border-radius: 50%;
  border: 3rpx solid $primary-color;
  margin-right: 14rpx;
}

.nutritionist-name {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.nutritionist-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #999;
}

.drawer-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1000;
  background: rgba(61, 41, 50, 0.34);
  display: flex;
}

.history-drawer {
  width: 76%;
  height: 100vh;
  background: #FFFBFD;
  display: flex;
  flex-direction: column;
  box-shadow: 16rpx 0 40rpx rgba(61, 41, 50, 0.14);
  animation: drawer-in 0.22s ease-out;
}

@keyframes drawer-in {
  from { transform: translateX(-100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  padding: 72rpx 28rpx 24rpx;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  border-bottom: 1rpx solid rgba(255, 182, 193, 0.26);
}

.drawer-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #3D2932;
}

.drawer-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #A58A95;
}

.drawer-close {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #FFF0F5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drawer-close-icon {
  width: 34rpx;
  height: 34rpx;
}

.history-scroll {
  flex: 1;
  padding: 20rpx 18rpx;
}

.history-status {
  padding: 80rpx 0;
  text-align: center;
  font-size: 25rpx;
  color: #B895A3;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 22rpx 18rpx;
  margin-bottom: 12rpx;
  border-radius: 24rpx;
  background: transparent;
}

.history-item.active {
  background: #FFF0F5;
}

.history-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 18rpx;
  background: #FFF4F8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.history-icon-svg {
  width: 42rpx;
  height: 42rpx;
}

.history-info {
  flex: 1;
  min-width: 0;
  margin-left: 16rpx;
}

.history-title {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 27rpx;
  color: #553A46;
}

.history-time {
  display: block;
  margin-top: 7rpx;
  font-size: 21rpx;
  color: #B895A3;
}

.new-chat-footer {
  margin: 18rpx 24rpx 34rpx;
  height: 88rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, $primary-color 0%, #FF8DC2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 24rpx rgba(255, 105, 180, 0.2);
}

.new-chat-plus {
  color: #FFFFFF;
  font-size: 38rpx;
  margin-right: 8rpx;
}

.new-chat-text {
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 700;
}

.chat-scroll {
  flex: 1;
  padding: 24rpx;
}

.message-item {
  margin-bottom: 32rpx;
}

.message-content {
  display: flex;
  align-items: flex-start;
}

.message-user {
  justify-content: flex-end;
}

.message-ai {
  justify-content: flex-start;
}

.msg-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.message-user .msg-avatar {
  margin-left: 16rpx;
}

.message-ai .msg-avatar {
  margin-right: 16rpx;
}

.msg-bubble {
  max-width: 70%;
}

.message-user .msg-bubble {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 32rpx 32rpx 8rpx 32rpx;
  padding: 24rpx;
}

.message-ai .msg-bubble {
  background: $card-bg;
  border-radius: 32rpx 32rpx 32rpx 8rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(255, 182, 193, 0.1);
}

.msg-text {
  font-size: 30rpx;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-user .msg-text {
  color: #FFFFFF;
}

.message-ai .msg-text {
  color: $primary-color;
}

.msg-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.msg-image {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
}

.loading-item {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 32rpx;
}

.loading-content {
  display: flex;
  align-items: center;
}

.loading-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-right: 16rpx;
}

.loading-dots {
  display: flex;
  gap: 8rpx;
  padding: 24rpx 32rpx;
  background: $card-bg;
  border-radius: 32rpx;
}

.dot {
  width: 16rpx;
  height: 16rpx;
  background: $primary-color;
  border-radius: 50%;
  animation: dotPulse 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes dotPulse {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.disclaimer {
  text-align: center;
  padding: 16rpx 24rpx;
  background: rgba(255, 215, 0, 0.1);
  border-top: 1rpx solid rgba(255, 215, 0, 0.3);
}

.disclaimer-text {
  font-size: 22rpx;
  color: #FF8C00;
}

.input-area {
  background: $card-bg;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 16rpx;
  max-height: 200rpx;
  overflow-y: auto;
}

.preview-item {
  width: 140rpx;
  height: 140rpx;
  border-radius: 20rpx;
  overflow: hidden;
  position: relative;
}

.preview-image {
  width: 100%;
  height: 100%;
}

.preview-delete {
  position: absolute;
  top: -16rpx;
  right: -16rpx;
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-icon {
  width: 40rpx;
  height: 40rpx;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
}

.image-btn {
  width: 88rpx;
  height: 88rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.image-btn:active {
  transform: scale(0.95);
}

.image-icon {
  width: 44rpx;
  height: 44rpx;
}

.text-input {
  flex: 1;
  min-height: 88rpx;
  max-height: 200rpx;
  background: rgba(255, 182, 193, 0.1);
  border-radius: 44rpx;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  color: #333;
  border: 2rpx solid rgba(255, 182, 193, 0.3);
  box-sizing: border-box;
}

.send-btn {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 44rpx;
  padding: 24rpx 48rpx;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.send-btn:active {
  transform: scale(0.95);
}

.send-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}

.send-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #FFFFFF;
}
</style>
