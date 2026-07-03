<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { wxLogin, accountLogin } from '@/api'

const userStore = useUserStore()

onLaunch(async () => {
  userStore.restoreLogin()
  if (!userStore.isLoggedIn) {
    await doAutoLogin()
  }
})

onShow(() => {
  if (!userStore.isLoggedIn) {
    doAutoLogin()
  }
})

async function doAutoLogin() {
  try {
    uni.showLoading({ title: '登录中...', mask: true })
    const platform = process.env.UNI_PLATFORM as string
    if (platform === 'mp-weixin') {
      const loginRes = await uni.login({})
      if (loginRes.code) {
        const apiRes = await wxLogin(loginRes.code)
        userStore.setLogin(apiRes.data.token, apiRes.data.user)
        uni.showToast({ title: '登录成功', icon: 'success' })
      }
    } else {
      const apiRes = await accountLogin('test', '123456')
      userStore.setLogin(apiRes.data.token, apiRes.data.user)
      uni.showToast({ title: 'H5 测试登录成功', icon: 'success' })
    }
  } catch (e) {
    console.error('登录失败:', e)
    uni.showToast({ title: '登录失败，请检查网络', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss">
@import 'uview-plus/index.scss';
@import './styles/common.scss';
</style>
