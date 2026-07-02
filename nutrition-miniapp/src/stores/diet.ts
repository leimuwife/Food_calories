import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 饮食记录项 */
export interface DietItem {
  id?: number
  recordId?: number
  foodId: number
  foodName: string
  weight: number
  calories: number
  protein: number
  fat: number
  carbs: number
}

/** 饮食记录 */
export interface DietRecord {
  id?: number
  recordDate: string
  mealType: 'breakfast' | 'lunch' | 'dinner' | 'snack'
  items: DietItem[]
  remark?: string
}

/** 当日营养汇总 */
export interface DailySummary {
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
  calorieGoal: number
  proteinGoal: number
  fatGoal: number
  carbsGoal: number
  caloriePercent: number
  proteinPercent: number
  fatPercent: number
  carbsPercent: number
  meals: Record<string, { calories: number; protein: number; fat: number; carbs: number; items: DietItem[] }>
}

export const useDietStore = defineStore('diet', () => {
  const todayRecords = ref<DietRecord[]>([])
  const todaySummary = ref<DailySummary | null>(null)
  const loading = ref(false)

  /** 餐次名称映射 */
  const mealTypeMap: Record<string, string> = {
    breakfast: '早餐',
    lunch: '午餐',
    dinner: '晚餐',
    snack: '加餐',
  }

  /** 获取餐次中文名 */
  function getMealName(type: string): string {
    return mealTypeMap[type] || type
  }

  /** 设置今日数据 */
  function setTodayData(records: DietRecord[], summary: DailySummary) {
    todayRecords.value = records
    todaySummary.value = summary
  }

  /** 清除数据 */
  function clear() {
    todayRecords.value = []
    todaySummary.value = null
  }

  return {
    todayRecords,
    todaySummary,
    loading,
    mealTypeMap,
    getMealName,
    setTodayData,
    clear,
  }
})
