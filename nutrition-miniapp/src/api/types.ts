export interface UserVO {
  id: number
  openid: string
  nickname: string
  fileIds: string | null
  email: string
  dailyCalorieGoal: number
  dailyProteinGoal: number
  dailyFatGoal: number
  dailyCarbsGoal: number
}

export interface LoginResultVO {
  token: string
  user: UserVO
}

export interface FoodVO {
  id: number
  foodName: string
  category: string
  caloriesPer100g: number
  proteinPer100g: number
  fatPer100g: number
  carbsPer100g: number
  fiberPer100g: number
  ediblePortion: number
  dataSource: string
}

export interface FoodCategoryVO {
  category: string
}

export interface FoodSearchResultVO {
  list: FoodVO[]
  total: number
}

export interface DietItemVO {
  id: number
  recordId: number
  foodId: number
  foodName: string
  weight: number
  calories: number
  protein: number
  fat: number
  carbs: number
}

export interface DietRecordVO {
  id: number
  recordDate: string
  mealType: 'breakfast' | 'lunch' | 'dinner' | 'snack'
  items: DietItemVO[]
  remark?: string
}

export interface DailySummaryVO {
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
  proteinRatio: number
  fatRatio: number
  carbsRatio: number
  meals: Record<string, {
    calories: number
    protein: number
    fat: number
    carbs: number
    items: DietItemVO[]
  }>
}

export interface DailyDietVO {
  records: DietRecordVO[]
  summary: DailySummaryVO
}

export interface MonthlySummaryVO {
  avgDailyCalories: number
  avgProteinRatio: number
  avgFatRatio: number
  avgCarbsRatio: number
  dailyTrend: DailyTrendVO[]
  topFoods: TopFoodVO[]
  totalDays: number
}

export interface DailyTrendVO {
  day: number
  calories: number
}

export interface TopFoodVO {
  foodName: string
  count: number
}

export interface LoginParam {
  username: string
  password: string
}

export interface RegisterParam {
  username: string
  password: string
  nickname: string
}

export interface WxLoginParam {
  code: string
}

export interface DietRecordParam {
  recordDate: string
  mealType: string
  items: { foodId: number; foodName: string; weight: number }[]
  remark?: string
}

export interface DietItemUpdateParam {
  weight: number
}

export interface GoalUpdateParam {
  dailyCalorieGoal?: number
  dailyProteinGoal?: number
  dailyFatGoal?: number
  dailyCarbsGoal?: number
}

export type NutritionGoalUpdateParam = GoalUpdateParam

export interface ProfileUpdateParam {
  nickname?: string
  fileIds?: string
  email?: string
}
