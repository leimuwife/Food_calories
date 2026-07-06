<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onLaunch(async () => {
  userStore.restoreLogin()
})

onShow(() => {
  userStore.restoreLogin()
})
</script>

<style lang="scss">
@import 'uview-plus/index.scss';
@import './styles/common.scss';
</style>
