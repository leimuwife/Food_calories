<template>
  <view class="page-container">
    <view class="header-area">
      <view class="back-btn" @tap="goBack">
        <svg viewBox="0 0 48 48" class="back-icon">
          <circle cx="24" cy="24" r="22" fill="#FFFFFF"/>
          <path d="M28 18 L20 24 L28 30" stroke="#FF69B4" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="18" cy="24" r="3" fill="#FF69B4"/>
          <circle cx="19" cy="23" r="1" fill="#fff"/>
        </svg>
      </view>
      <text class="header-title">轻友圈</text>
      <view class="publish-btn" @tap="goToPublish">
        <view class="publish-icon-wrap">
          <svg viewBox="0 0 48 48" class="publish-icon">
            <circle cx="24" cy="24" r="20" fill="#FF69B4"/>
            <path d="M24 16 L24 32 M16 24 L32 24" stroke="#FFFFFF" stroke-width="3" fill="none" stroke-linecap="round"/>
          </svg>
        </view>
        <text class="publish-text">发布动态</text>
      </view>
    </view>

    <scroll-view 
      scroll-y 
      class="feed-scroll"
      @refreshrefresh="onRefresh"
      @scrolltolower="onLoadMore"
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
    >
      <view v-if="feedList.length === 0" class="empty-state">
        <svg viewBox="0 0 120 120" class="empty-icon">
          <circle cx="60" cy="60" r="50" fill="#FFB6C1"/>
          <circle cx="45" cy="50" r="5" fill="#333"/>
          <circle cx="75" cy="50" r="5" fill="#333"/>
          <circle cx="46" cy="49" r="1.5" fill="#fff"/>
          <circle cx="76" cy="49" r="1.5" fill="#fff"/>
          <path d="M60 62 Q57 66 60 70 Q63 66 60 62" stroke="#333" stroke-width="2" fill="none"/>
          <path d="M30 80 L90 80" stroke="#FF69B4" stroke-width="3" fill="none"/>
          <circle cx="35" cy="78" r="4" fill="#FF69B4"/>
          <circle cx="45" cy="82" r="3" fill="#FF69B4"/>
          <circle cx="55" cy="78" r="3" fill="#FF69B4"/>
          <circle cx="65" cy="82" r="4" fill="#FF69B4"/>
          <circle cx="75" cy="78" r="3" fill="#FF69B4"/>
          <circle cx="85" cy="82" r="3" fill="#FF69B4"/>
        </svg>
        <text class="empty-text">还没有动态，快去发布吧~</text>
      </view>

      <view v-for="feed in feedList" :key="feed.id" class="feed-card">
        <view class="feed-header">
          <image :src="getAvatarUrl(feed.userAvatar)" class="user-avatar" mode="aspectFill"/>
          <view class="user-info">
            <text class="user-name">{{ feed.userName }}</text>
            <text class="publish-time">{{ feed.publishTime }}</text>
          </view>
        </view>

        <view v-if="feed.content" class="feed-content">
          <text class="content-text">{{ feed.content }}</text>
        </view>

        <view v-if="feed.fileIds && feed.fileIds.length > 0" class="feed-images">
          <view 
            v-for="(img, idx) in getFeedImages(feed.fileIds)" 
            :key="idx" 
            :class="getImageClass(feed.fileIds.length, idx)"
            @tap="previewImage(getFeedImages(feed.fileIds), idx)"
          >
            <image :src="img" class="feed-image" mode="aspectFill"/>
          </view>
        </view>

        <view class="feed-actions">
          <view :class="['action-btn', { 'action-btn-active': feed.isLiked }]" @tap="toggleLike(feed)">
            <svg viewBox="0 0 48 48" class="action-icon">
              <path d="M24 42 C12 36 4 28 4 18 C4 10 10 4 17 4 C20 4 23 6 24 7 C25 6 28 4 31 4 C38 4 44 10 44 18 C44 28 36 36 24 42 Z" :fill="feed.isLiked ? '#FF69B4' : '#CCC'" stroke="#FF69B4" stroke-width="2"/>
            </svg>
            <text class="action-text">{{ feed.likeCount }} 点赞</text>
          </view>
          <view class="action-btn" @tap="openComment(feed)">
            <svg viewBox="0 0 48 48" class="action-icon">
              <rect x="8" y="12" width="32" height="24" rx="4" fill="#CCC" stroke="#FF69B4" stroke-width="2"/>
              <circle cx="16" cy="20" r="3" fill="#FFB6C1"/>
              <circle cx="28" cy="20" r="3" fill="#FFB6C1"/>
              <path d="M16 26 Q24 30 32 26" stroke="#FF69B4" stroke-width="2" fill="none"/>
            </svg>
            <text class="action-text">{{ feed.commentCount }} 评论</text>
          </view>
        </view>

        <view v-if="feed.comments && feed.comments.length > 0" class="feed-comments">
          <view v-for="(comment, idx) in getDisplayComments(feed)" :key="idx" class="comment-item">
            <text class="comment-name">{{ comment.userName }}：</text>
            <text class="comment-text">{{ comment.content }}</text>
          </view>
          <view v-if="feed.comments.length > 3" class="view-all-comments" @tap="openComment(feed)">
            <text class="view-all-text">查看全部 {{ feed.comments.length }} 条评论</text>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-more">
        <text class="loading-text">加载中...</text>
      </view>
      <view v-if="!loading && !hasMore" class="no-more">
        <text class="no-more-text">- 已经到底啦 -</text>
      </view>
    </scroll-view>

    <view v-if="showCommentPopup" class="comment-overlay" @tap="closeComment">
      <view class="comment-popup" @tap.stop>
        <view class="comment-header">
          <text class="comment-title">评论</text>
          <view class="comment-close" @tap="closeComment">
            <svg viewBox="0 0 48 48" class="close-icon">
              <path d="M16 16 L32 32 M32 16 L16 32" stroke="#999" stroke-width="3" fill="none" stroke-linecap="round"/>
            </svg>
          </view>
        </view>
        <scroll-view scroll-y class="comment-list">
          <view v-for="(comment, idx) in currentFeed?.comments" :key="idx" class="comment-item">
            <text class="comment-name">{{ comment.userName }}：</text>
            <text class="comment-text">{{ comment.content }}</text>
          </view>
          <view v-if="!currentFeed?.comments || currentFeed.comments.length === 0" class="empty-comments">
            <text class="empty-comment-text">暂无评论</text>
          </view>
        </scroll-view>
        <view class="comment-input-area">
          <input v-model="commentInput" class="comment-input" placeholder="写下你的评论..." @confirm="submitComment"/>
          <view :class="['send-btn', { 'send-btn-disabled': !commentInput.trim() }]" @tap="submitComment">
            <text class="send-text">发送</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { FeedItem, FeedComment } from '@/api/types'
import { getFeedList, toggleFeedLike, addFeedComment } from '@/api/qingyouquan/qingyouquan'

const feedList = ref<FeedItem[]>([])
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const hasMore = ref(true)
const isRefreshing = ref(false)
const showCommentPopup = ref(false)
const commentInput = ref('')
const currentFeed = ref<FeedItem | null>(null)

function getAvatarUrl(avatar: string | null): string {
  if (!avatar) {
    return '/static/images/AI/nutritionist.png'
  }
  let firstId: string | null = null
  try {
    const ids = JSON.parse(avatar)
    if (Array.isArray(ids) && ids.length > 0) {
      firstId = String(ids[0])
    }
  } catch {
    const parts = avatar.split(',')
    if (parts.length > 0) {
      firstId = parts[0].trim()
    }
  }
  return firstId ? `/api/attachment/${firstId}/url` : '/static/images/AI/nutritionist.png'
}

function getFeedImages(fileIds: string[]): string[] {
  return fileIds.map(id => `/api/attachment/${id}/url`)
}

function getImageClass(total: number, index: number): string {
  const classes = ['image-item']
  if (total === 1) {
    classes.push('image-single')
  } else if (total === 2 || total === 4) {
    classes.push('image-double')
  } else {
    classes.push('image-grid')
  }
  return classes.join(' ')
}

function previewImage(images: string[], index: number) {
  uni.previewImage({
    urls: images,
    current: images[index]
  })
}

function getDisplayComments(feed: FeedItem): FeedComment[] {
  return feed.comments.slice(0, 3)
}

async function toggleLike(feed: FeedItem) {
  try {
    await toggleFeedLike(feed.id)
    feed.isLiked = !feed.isLiked
    feed.likeCount += feed.isLiked ? 1 : -1
  } catch (e) {
    console.error('点赞失败:', e)
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

function openComment(feed: FeedItem) {
  currentFeed.value = feed
  showCommentPopup.value = true
  commentInput.value = ''
}

function closeComment() {
  showCommentPopup.value = false
  currentFeed.value = null
}

async function submitComment() {
  if (!commentInput.value.trim() || !currentFeed.value) return
  
  try {
    await addFeedComment(currentFeed.value.id, commentInput.value.trim())
    
    const newComment: FeedComment = {
      id: Date.now(),
      userId: 0,
      userName: '我',
      content: commentInput.value.trim(),
      createTime: '刚刚'
    }
    
    currentFeed.value.comments.unshift(newComment)
    currentFeed.value.commentCount++
    commentInput.value = ''
    
    uni.showToast({ title: '评论成功', icon: 'success' })
  } catch (e) {
    console.error('评论失败:', e)
    uni.showToast({ title: '评论失败', icon: 'none' })
  }
}

async function loadFeedList(isRefresh = false) {
  if (loading.value) return
  
  loading.value = true
  
  if (isRefresh) {
    pageNum.value = 1
    hasMore.value = false
  }
  
  try {
    const res = await getFeedList(pageNum.value, pageSize)
    const data = res.data
    
    feedList.value = data.list || []
    hasMore.value = data.hasMore !== false
  } catch (e) {
    console.error('加载动态失败:', e)
    
    feedList.value = [
        {
          id: Date.now() - 10000,
          userId: 1,
          userName: '张三',
          userAvatar: null,
          content: '今天的早餐很丰盛！水煮西兰花 + 全麦面包 + 牛奶，热量控制得很好～',
          fileIds: [],
          likeCount: 23,
          isLiked: false,
          commentCount: 5,
          comments: [
            { id: 1, userId: 2, userName: '李四', content: '看起来好好吃！', createTime: '10分钟前' },
            { id: 2, userId: 3, userName: '营养师', content: '搭配很均衡，继续保持！', createTime: '20分钟前' },
            { id: 3, userId: 4, userName: '小红', content: '请问西兰花是水煮还是清蒸呀？', createTime: '30分钟前' }
          ],
          publishTime: '1小时前',
          createTime: new Date().toISOString()
        },
        {
          id: Date.now() - 20000,
          userId: 2,
          userName: '李四',
          userAvatar: null,
          content: '打卡第7天！体重下降了3斤，好开心～',
          fileIds: [],
          likeCount: 56,
          isLiked: true,
          commentCount: 8,
          comments: [
            { id: 4, userId: 1, userName: '张三', content: '太棒了！恭喜恭喜', createTime: '5分钟前' },
            { id: 5, userId: 5, userName: '小明', content: '求分享减肥经验！', createTime: '15分钟前' }
          ],
          publishTime: '2小时前',
          createTime: new Date().toISOString()
        },
        {
          id: Date.now() - 30000,
          userId: 3,
          userName: '小张营养师',
          userAvatar: null,
          content: '今日营养小贴士：晚餐尽量在7点前吃完，给肠胃足够的消化时间～',
          fileIds: [],
          likeCount: 89,
          isLiked: false,
          commentCount: 12,
          comments: [
            { id: 6, userId: 1, userName: '张三', content: '学到了！', createTime: '刚刚' },
          { id: 7, userId: 2, userName: '李四', content: '感谢营养师的建议', createTime: '3分钟前' },
          { id: 8, userId: 6, userName: '小华', content: '坚持了一周，效果不错', createTime: '8分钟前' },
          { id: 9, userId: 7, userName: '小雪', content: '真的有用！', createTime: '12分钟前' }
        ],
        publishTime: '3小时前',
        createTime: new Date().toISOString()
      }
    ]
    
    hasMore.value = false
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

function onRefresh() {
  isRefreshing.value = true
  loadFeedList(true)
}

function onLoadMore() {
  if (!loading.value && hasMore.value) {
    pageNum.value++
    loadFeedList()
  }
}

function goBack() {
  uni.navigateBack({ delta: 1 })
}

function goToPublish() {
  uni.navigateTo({ url: '/pages/qingyouquan/publish' })
}

onMounted(() => {
  loadFeedList()
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 32rpx;
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
}

.back-btn {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.back-btn:active {
  transform: scale(0.95);
}

.back-icon {
  width: 64rpx;
  height: 64rpx;
}

.header-title {
  font-size: 44rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.publish-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.publish-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}

.publish-icon-wrap:active {
  transform: scale(0.95);
}

.publish-icon {
  width: 48rpx;
  height: 48rpx;
}

.publish-text {
  font-size: 22rpx;
  color: #FFFFFF;
}

.feed-scroll {
  height: calc(100vh - 180rpx);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 32rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.feed-card {
  background: $card-bg;
  margin: 24rpx;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 182, 193, 0.08);
}

.feed-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.user-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #F5F5F5;
  margin-right: 20rpx;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.publish-time {
  font-size: 24rpx;
  color: #999;
}

.feed-content {
  margin-bottom: 24rpx;
}

.content-text {
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
}

.feed-images {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.image-item {
  border-radius: 20rpx;
  overflow: hidden;
}

.image-single {
  width: 100%;
  aspect-ratio: 1;
}

.image-double {
  width: calc(50% - 8rpx);
  aspect-ratio: 1;
}

.image-grid {
  width: calc(33.33% - 10.67rpx);
  aspect-ratio: 1;
}

.feed-image {
  width: 100%;
  height: 100%;
}

.feed-actions {
  display: flex;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 182, 193, 0.3);
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 20rpx 0;
  transition: all 0.2s;
}

.action-btn:active {
  transform: scale(0.98);
}

.action-btn-active .action-icon path {
  fill: $primary-color;
}

.action-btn-active .action-text {
  color: $primary-color;
}

.action-icon {
  width: 40rpx;
  height: 40rpx;
}

.action-text {
  font-size: 28rpx;
  color: #666;
}

.feed-comments {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid rgba(255, 182, 193, 0.3);
}

.comment-item {
  display: flex;
  margin-bottom: 16rpx;
}

.comment-name {
  font-size: 26rpx;
  color: $primary-color;
  font-weight: 500;
  margin-right: 8rpx;
}

.comment-text {
  font-size: 26rpx;
  color: #666;
}

.view-all-comments {
  padding: 12rpx 0;
}

.view-all-text {
  font-size: 26rpx;
  color: $primary-color;
}

.loading-more, .no-more {
  text-align: center;
  padding: 32rpx 0;
}

.loading-text, .no-more-text {
  font-size: 26rpx;
  color: #999;
}

.comment-overlay {
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

.comment-popup {
  width: 100%;
  background: $card-bg;
  border-radius: 48rpx 48rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  animation: slideUp 0.3s ease;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 48rpx;
  border-bottom: 1rpx solid rgba(255, 182, 193, 0.3);
}

.comment-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $primary-color;
}

.comment-close {
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

.comment-list {
  flex: 1;
  padding: 24rpx 48rpx;
  max-height: 480rpx;
}

.empty-comments {
  text-align: center;
  padding: 48rpx 0;
}

.empty-comment-text {
  font-size: 28rpx;
  color: #999;
}

.comment-input-area {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 48rpx;
  border-top: 1rpx solid rgba(255, 182, 193, 0.3);
  background: $bg-color;
}

.comment-input {
  flex: 1;
  height: 80rpx;
  background: $card-bg;
  border-radius: 40rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
  border: 2rpx solid rgba(255, 182, 193, 0.3);
}

.send-btn {
  background: linear-gradient(135deg, $primary-color 0%, $light-pink 100%);
  border-radius: 40rpx;
  padding: 20rpx 48rpx;
  transition: all 0.2s;
}

.send-btn:active {
  transform: scale(0.95);
}

.send-btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}

.send-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #FFFFFF;
}
</style>