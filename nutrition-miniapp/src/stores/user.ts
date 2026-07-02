import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserVO } from '@/api/types'

export type UserInfo = UserVO

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  function restoreLogin() {
    try {
      const savedToken = uni.getStorageSync('token')
      const savedUser = uni.getStorageSync('userInfo')
      if (savedToken && savedUser) {
        token.value = savedToken
        userInfo.value = JSON.parse(savedUser)
      }
    } catch (e) {
      console.error('[UserStore] restoreLogin error:', e)
    }
  }

  function setLogin(t: string, user: UserInfo) {
    token.value = t
    userInfo.value = user
    uni.setStorageSync('token', t)
    uni.setStorageSync('userInfo', JSON.stringify(user))
  }

  function updateUser(user: Partial<UserInfo>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...user }
      uni.setStorageSync('userInfo', JSON.stringify(userInfo.value))
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    restoreLogin,
    setLogin,
    updateUser,
    logout,
  }
})
