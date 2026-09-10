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

export interface UserVO {
  id: number
  openid: string
  nickname: string
  fileIds: string | null
  avatarUrl: string | null
  dailyCalorieGoal: number
  dailyProteinGoal: number
  dailyFatGoal: number
  dailyCarbsGoal: number
}

export interface LoginResultVO {
  token: string
  user: UserVO
}

export enum MealType {
  BREAKFAST = 'breakfast',
  LUNCH = 'lunch',
  DINNER = 'dinner',
  SNACK = 'snack'
}

export const MealTypeNameMap: Record<MealType, string> = {
  [MealType.BREAKFAST]: '早餐',
  [MealType.LUNCH]: '午餐',
  [MealType.DINNER]: '晚餐',
  [MealType.SNACK]: '夜宵'
}

export interface DietItemVO {
  id: number | string
  recordId: number | string
  foodName: string
  foodDesc?: string
  weight: number | string
  calories: number | string
  remark?: string
  imageUrls?: string[]
}

export interface DietRecordVO {
  id: number | string
  recordDate: string
  mealType: MealType | string
  mealTypeName?: string
  totalCalories?: number | string
  items: DietItemVO[]
}

export interface DailySummaryVO {
  totalCalories: number
  calorieGoal: number
  caloriePercent: number
  meals: Record<string, {
    calories: number
    items: DietItemVO[]
  }>
}

export interface DailyDietVO {
  totalCalories: number | string
  breakfastCalories: number | string
  lunchCalories: number | string
  dinnerCalories: number | string
  snackCalories: number | string
  records: DietRecordVO[]
  foodList?: DietItemVO[]
  summary?: DailySummaryVO
}

export interface MonthlySummaryVO {
  avgDailyCalories: number
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
  mealType: MealType
  items: { id?: string | number; foodName: string; foodDesc?: string; weight: number; calories: number; remark?: string; fileIds?: string }[]
  remark?: string
}

export interface GoalUpdateParam {
  dailyCalorieGoal?: number
}

export type NutritionGoalUpdateParam = GoalUpdateParam

export interface ProfileUpdateParam {
  nickname?: string
  fileIds?: string
}

export interface FeedComment {
  id: number
  userId: number
  userName: string
  content: string
  createTime: string
}

export interface FeedItem {
  id: number
  userId: number
  userName: string
  userAvatar: string | null
  content: string
  fileIds: string[]
  imageUrls: string[]
  likeCount: number
  isLiked: boolean
  commentCount: number
  comments: FeedComment[]
  publishTime: string
  createTime: string
}

export interface FeedPublishParam {
  content: string
  fileIds: string[]
}

export interface FeedListResult {
  list: FeedItem[]
  total: number
  hasMore: boolean
}

export interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  images?: string[]
  fileIds?: string[]
  createTime: string
}

export interface NutritionistChatParam {
  content: string
  fileIds?: string[]
  /** 会话ID；不传表示新建对话，后端生成后在响应sessionId中回传 */
  sessionId?: string
}

/** 营养师对话响应：AI回答文本 + 会话标识（首次对话为新生成的会话ID） */
export interface NutritionistChatResult {
  response: string
  sessionId?: string
  newSession?: boolean
}

export interface UserFeedback {
  id: number
  userId: number
  feedbackContent: string
  feedbackStatus: number
  feedbackStatusDesc?: string
  adminReply?: string
  createTime: string
  updateTime: string
}

export enum FeedbackStatus {
  PENDING = 0,
  PROCESSING = 1,
  COMPLETED = 2
}

export const FeedbackStatusMap: Record<number, string> = {
  [FeedbackStatus.PENDING]: '待处理',
  [FeedbackStatus.PROCESSING]: '处理中',
  [FeedbackStatus.COMPLETED]: '已完结'
}
