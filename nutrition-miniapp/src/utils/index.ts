/**
 * 工具函数
 */

import dayjs from 'dayjs'

/** 格式化日期为 YYYY-MM-DD */
export function formatDate(date: Date | string): string {
  return dayjs(date).format('YYYY-MM-DD')
}

/** 获取今天日期 */
export function getToday(): string {
  return dayjs().format('YYYY-MM-DD')
}

/** 格式化日期显示 */
export function formatDateDisplay(date: string): string {
  return dayjs(date).format('MM月DD日')
}

/** 获取当前时间 */
export function getNowTime(): string {
  return dayjs().format('HH:mm')
}

/** 餐次类型 */
export type MealType = 'breakfast' | 'lunch' | 'dinner' | 'snack'

/** 餐次名称映射 */
export const MEAL_TYPE_MAP: Record<MealType, string> = {
  breakfast: '早餐',
  lunch: '午餐',
  dinner: '晚餐',
  snack: '加餐',
}

/** 餐次选项列表 */
export const MEAL_OPTIONS: { label: string; value: MealType }[] = [
  { label: '早餐', value: 'breakfast' },
  { label: '午餐', value: 'lunch' },
  { label: '晚餐', value: 'dinner' },
  { label: '加餐', value: 'snack' },
]

/** 食物分类颜色映射 */
export const CATEGORY_COLORS: Record<string, string> = {
  '主食': '#FFA94D',
  '肉蛋奶': '#FF6B6B',
  '蔬菜': '#51CF66',
  '水果': '#9775FA',
  '零食': '#F06595',
  '饮品': '#4DABF7',
  '调味品': '#868E96',
  '其他': '#ADB5BD',
}

/** 根据营养素克数计算热量 */
export function calcCalories(protein: number, fat: number, carbs: number): number {
  return Math.round(protein * 4 + fat * 9 + carbs * 4)
}

/** 计算供能占比 */
export function calcEnergyRatio(protein: number, fat: number, carbs: number) {
  const totalCal = calcCalories(protein, fat, carbs)
  if (totalCal === 0) return { proteinRatio: 0, fatRatio: 0, carbsRatio: 0 }
  return {
    proteinRatio: Math.round((protein * 4 / totalCal) * 1000) / 10,
    fatRatio: Math.round((fat * 9 / totalCal) * 1000) / 10,
    carbsRatio: Math.round((carbs * 4 / totalCal) * 1000) / 10,
  }
}

/** 根据每100g营养素和食用重量计算实际摄入 */
export function calcActualIntake(nutritionPer100g: {
  caloriesPer100g: number
  proteinPer100g: number
  fatPer100g: number
  carbsPer100g: number
}, weight: number) {
  const factor = weight / 100
  return {
    calories: Math.round(nutritionPer100g.caloriesPer100g * factor),
    protein: Math.round(nutritionPer100g.proteinPer100g * factor * 10) / 10,
    fat: Math.round(nutritionPer100g.fatPer100g * factor * 10) / 10,
    carbs: Math.round(nutritionPer100g.carbsPer100g * factor * 10) / 10,
    weight,
  }
}

/** 生成随机 ID（游客模式本地存储用） */
export function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 9)
}
