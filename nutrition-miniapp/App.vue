<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onLaunch(() => {
  console.log('[App] 营养助手启动')
  // 尝试从本地恢复登录态
  userStore.restoreLogin()
})
</script>

<style lang="scss">
@import 'uview-plus/index.scss';
@import '@/styles/common.scss';

/* 全局样式 */
page {
  background-color: #F5F7FA;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #303133;
}
</style>
