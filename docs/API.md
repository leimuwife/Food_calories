# 食光笔记 后端接口开发需求文档

## 基本信息

- **后端服务地址**: `http://localhost:8088`
- **全局路径前缀**: `/api`
- **响应格式**:
```json
{
  "code": "200",
  "message": "操作成功",
  "data": {}
}
```
- **认证方式**: JWT Token（Bearer Token）
- **技术栈**: Spring Boot + MyBatis-Plus + MySQL

---

## 一、已实现接口

以下接口已实现，无需再开发：

| 序号 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 1 | `/api/auth/login` | POST | 用户登录 |
| 2 | `/api/auth/register` | POST | 用户注册 |
| 3 | `/api/auth/wx-login` | POST | 微信登录 |
| 4 | `/api/user/profile` | GET | 获取用户信息 |
| 5 | `/api/user/profile` | PUT | 更新个人信息 |

---

## 二、待开发接口清单

### 2.1 饮食记录模块

#### 2.1.1 查询今日饮食记录

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/diet/record` |
| **认证** | 需要 |
| **所属页面** | 首页、饮食历史记录页 |
| **前端调用位置** | `src/api/shouye/index.ts`, `src/api/history/history.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| date | String | 是 | 查询日期，格式：YYYY-MM-DD |

**返回参数** (`DailyDietVO`):
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "recordDate": "2026-07-07",
        "mealType": "breakfast",
        "items": [
          {
            "id": 1,
            "recordId": 1,
            "foodName": "牛奶",
            "foodDesc": "250ml纯牛奶",
            "weight": 250,
            "calories": 150,
            "remark": ""
          }
        ]
      }
    ],
    "summary": {
      "totalCalories": 1500,
      "calorieGoal": 2000,
      "caloriePercent": 75,
      "meals": {
        "breakfast": {
          "calories": 400,
          "items": []
        },
        "lunch": {
          "calories": 600,
          "items": []
        }
      }
    }
  }
}
```

**数据模型**:
```typescript
interface DietItemVO {
  id: number           // 饮食项ID
  recordId: number     // 记录ID
  foodName: string     // 食物名称
  foodDesc?: string    // 食物描述
  weight: number       // 重量(g)
  calories: number     // 热量(kcal)
  remark?: string      // 备注
}

interface DietRecordVO {
  id: number           // 记录ID
  recordDate: string   // 记录日期 YYYY-MM-DD
  mealType: string     // 餐段类型：breakfast/lunch/dinner/snack
  items: DietItemVO[]  // 饮食项列表
}

interface DailySummaryVO {
  totalCalories: number      // 今日总热量
  calorieGoal: number        // 热量目标
  caloriePercent: number     // 完成百分比
  meals: Record<string, {    // 各餐段统计
    calories: number
    items: DietItemVO[]
  }>
}

interface DailyDietVO {
  records: DietRecordVO[]    // 饮食记录列表
  summary: DailySummaryVO    // 今日统计
}
```

---

#### 2.1.2 添加饮食记录

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/diet/record` |
| **认证** | 需要 |
| **所属页面** | 早餐/午餐/晚餐/夜宵添加页 |
| **前端调用位置** | `src/api/add/add.ts` |

**请求参数** (`DietRecordParam`):
```json
{
  "recordDate": "2026-07-07",
  "mealType": "breakfast",
  "items": [
    {
      "foodName": "牛奶",
      "foodDesc": "250ml纯牛奶",
      "weight": 250,
      "calories": 150,
      "remark": ""
    }
  ],
  "remark": "早餐记录"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| recordDate | String | 是 | 记录日期，格式：YYYY-MM-DD |
| mealType | String | 是 | 餐段类型：breakfast/lunch/dinner/snack |
| items | Array | 是 | 饮食项列表 |
| remark | String | 否 | 整体备注 |

**返回参数**:
```json
{
  "code": "200",
  "message": "添加成功",
  "data": {
    "recordId": 1
  }
}
```

---

#### 2.1.3 删除饮食记录

| 属性 | 值 |
|------|------|
| **路径** | `DELETE /api/diet/record/{recordId}` |
| **认证** | 需要 |
| **所属页面** | 饮食历史记录详情页 |
| **前端调用位置** | `src/api/add/add.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| recordId | Long | 是 | 记录ID（路径参数） |

**返回参数**:
```json
{
  "code": "200",
  "message": "删除成功",
  "data": null
}
```

---

#### 2.1.4 更新饮食项

| 属性 | 值 |
|------|------|
| **路径** | `PUT /api/diet/item/{itemId}` |
| **认证** | 需要 |
| **所属页面** | 饮食历史记录详情页 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**:
```json
{
  "weight": 300
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Long | 是 | 饮食项ID（路径参数） |
| weight | Integer | 是 | 新重量(g) |

**返回参数**:
```json
{
  "code": "200",
  "message": "更新成功",
  "data": null
}
```

---

#### 2.1.5 删除饮食项

| 属性 | 值 |
|------|------|
| **路径** | `DELETE /api/diet/item/{itemId}` |
| **认证** | 需要 |
| **所属页面** | 饮食历史记录详情页 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Long | 是 | 饮食项ID（路径参数） |

**返回参数**:
```json
{
  "code": "200",
  "message": "删除成功",
  "data": null
}
```

---

#### 2.1.6 复制记录到指定日期

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/diet/record/{recordId}/copy` |
| **认证** | 需要 |
| **所属页面** | 饮食历史记录详情页 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**:
```json
{
  "targetDate": "2026-07-08"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| recordId | Long | 是 | 源记录ID（路径参数） |
| targetDate | String | 是 | 目标日期，格式：YYYY-MM-DD |

**返回参数**:
```json
{
  "code": "200",
  "message": "复制成功",
  "data": {
    "newRecordId": 2
  }
}
```

---

#### 2.1.7 按日期范围查询记录

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/diet/records/range` |
| **认证** | 需要 |
| **所属页面** | 饮食历史记录页 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| startDate | String | 是 | 开始日期，格式：YYYY-MM-DD |
| endDate | String | 是 | 结束日期，格式：YYYY-MM-DD |

**返回参数**:
```json
{
  "code": "200",
  "message": "success",
  "data": [
    {
      "id": 1,
      "recordDate": "2026-07-01",
      "mealType": "breakfast",
      "items": []
    }
  ]
}
```

---

### 2.2 统计分析模块

#### 2.2.1 每日统计

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/statistics/daily` |
| **认证** | 需要 |
| **所属页面** | 首页、饮食历史记录页 |
| **前端调用位置** | `src/api/shouye/index.ts`, `src/api/index.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| date | String | 是 | 查询日期，格式：YYYY-MM-DD |

**返回参数**:
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "totalCalories": 1500,
    "totalProtein": 50,
    "totalFat": 30,
    "totalCarbs": 200,
    "calorieGoal": 2000,
    "proteinGoal": 60,
    "fatGoal": 40,
    "carbsGoal": 250,
    "mealTypes": ["breakfast", "lunch"],
    "caloriesByMeal": {
      "breakfast": 400,
      "lunch": 600
    },
    "proteinsByMeal": {
      "breakfast": 15,
      "lunch": 20
    },
    "fatsByMeal": {
      "breakfast": 10,
      "lunch": 15
    },
    "carbsByMeal": {
      "breakfast": 50,
      "lunch": 100
    },
    "meals": {
      "breakfast": {
        "items": [],
        "calories": 400,
        "protein": 15,
        "fat": 10,
        "carbs": 50
      }
    }
  }
}
```

---

#### 2.2.2 月度统计

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/statistics/monthly` |
| **认证** | 需要 |
| **所属页面** | 打卡页、饮食历史记录页 |
| **前端调用位置** | `src/api/daka/daka.ts`, `src/api/history/history.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份，如：2026 |
| month | Integer | 是 | 月份，如：7 |

**返回参数** (`MonthlySummaryVO`):
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "avgDailyCalories": 1500,
    "dailyTrend": [
      { "day": 1, "calories": 1400 },
      { "day": 2, "calories": 1600 }
    ],
    "topFoods": [
      { "foodName": "牛奶", "count": 10 },
      { "foodName": "面包", "count": 8 }
    ],
    "totalDays": 30
  }
}
```

**数据模型**:
```typescript
interface DailyTrendVO {
  day: number        // 日期（1-31）
  calories: number   // 当日总热量
}

interface TopFoodVO {
  foodName: string   // 食物名称
  count: number      // 出现次数
}

interface MonthlySummaryVO {
  avgDailyCalories: number   // 月均每日热量
  dailyTrend: DailyTrendVO[] // 每日热量趋势
  topFoods: TopFoodVO[]      // 最常吃食物排行
  totalDays: number          // 有记录的天数
}
```

---

#### 2.2.3 导出数据

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/statistics/export` |
| **认证** | 需要 |
| **所属页面** | 饮食历史记录页 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| startDate | String | 是 | 开始日期 |
| endDate | String | 是 | 结束日期 |

**返回参数**: 文件下载（Excel或CSV）

---

### 2.3 食物字典模块

#### 2.3.1 搜索食物

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/food/search` |
| **认证** | 不需要 |
| **所属页面** | 首页 |
| **前端调用位置** | `src/api/shouye/index.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |

**返回参数** (`FoodSearchResult`):
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "foodName": "牛奶",
        "category": "乳制品",
        "caloriesPer100g": 60,
        "proteinPer100g": 3.0,
        "fatPer100g": 3.2,
        "carbsPer100g": 4.8,
        "fiberPer100g": 0,
        "ediblePortion": 100,
        "dataSource": "中国食物成分表"
      }
    ],
    "total": 10
  }
}
```

**数据模型**:
```typescript
interface FoodVO {
  id: number              // 食物ID
  foodName: string        // 食物名称
  category: string        // 分类
  caloriesPer100g: number // 每100g热量(kcal)
  proteinPer100g: number  // 每100g蛋白质(g)
  fatPer100g: number      // 每100g脂肪(g)
  carbsPer100g: number    // 每100g碳水(g)
  fiberPer100g: number    // 每100g膳食纤维(g)
  ediblePortion: number   // 可食用部分比例(%)
  dataSource: string      // 数据来源
}

interface FoodSearchResult {
  list: FoodVO[]
  total: number
}
```

---

### 2.4 打卡模块

#### 2.4.1 打卡

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/checkin` |
| **认证** | 需要 |
| **所属页面** | 打卡页 |
| **前端调用位置** | `src/api/daka/daka.ts` |

**请求参数**:
```json
{
  "date": "2026-07-07"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| date | String | 是 | 打卡日期，格式：YYYY-MM-DD |

**返回参数**:
```json
{
  "code": "200",
  "message": "打卡成功",
  "data": null
}
```

---

#### 2.4.2 取消打卡

| 属性 | 值 |
|------|------|
| **路径** | `DELETE /api/checkin/{date}` |
| **认证** | 需要 |
| **所属页面** | 打卡页 |
| **前端调用位置** | `src/api/daka/daka.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| date | String | 是 | 打卡日期（路径参数） |

**返回参数**:
```json
{
  "code": "200",
  "message": "取消成功",
  "data": null
}
```

---

#### 2.4.3 查询月度打卡日期

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/checkin/monthly` |
| **认证** | 需要 |
| **所属页面** | 打卡页 |
| **前端调用位置** | `src/api/daka/daka.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份 |

**返回参数**:
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "dates": ["2026-07-01", "2026-07-02", "2026-07-07"]
  }
}
```

---

### 2.5 AI营养师模块

#### 2.5.1 对话

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/nutritionist/chat` |
| **认证** | 需要 |
| **所属页面** | AI营养师页 |
| **前端调用位置** | `src/api/yingyangshi/yingyangshi.ts` |

**请求参数** (`NutritionistChatParam`):
```json
{
  "content": "我今天吃了一碗米饭和两个鸡蛋，热量是多少？",
  "fileIds": ["123456789"]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是 | 用户消息内容 |
| fileIds | Array | 否 | 附件ID列表（图片） |

**返回参数** (`NutritionistChatResult`):
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "message": {
      "id": 1,
      "role": "assistant",
      "content": "一碗米饭约200g热量约230kcal，两个鸡蛋约140kcal...",
      "images": [],
      "fileIds": [],
      "createTime": "2026-07-07T10:30:00"
    }
  }
}
```

**数据模型**:
```typescript
interface ChatMessage {
  id: number
  role: 'user' | 'assistant'  // 角色
  content: string             // 消息内容
  images?: string[]           // 图片URL列表（废弃）
  fileIds?: string[]          // 附件ID列表
  createTime: string          // 创建时间
}

interface NutritionistChatParam {
  content: string
  fileIds?: string[]
}

interface NutritionistChatResult {
  message: ChatMessage
}
```

---

#### 2.5.2 AI估算热量

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/nutritionist/estimate-calories` |
| **认证** | 需要 |
| **所属页面** | 早餐/午餐/晚餐/夜宵添加页 |
| **前端调用位置** | `src/api/add/add.ts` |

**请求参数**:
```json
{
  "description": "200g水煮西兰花 + 1个全麦面包"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| description | String | 是 | 食物描述 |

**返回参数**:
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "calories": 180
  }
}
```

---

### 2.6 轻友圈模块

#### 2.6.1 获取动态列表

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/feed/list` |
| **认证** | 需要 |
| **所属页面** | 轻友圈列表页 |
| **前端调用位置** | `src/api/qingyouquan/qingyouquan.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 是 | 页码，从1开始 |
| pageSize | Integer | 是 | 每页条数 |

**返回参数** (`FeedListResult`):
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "userId": 1,
        "userName": "张三",
        "userAvatar": "http://xxx/avatar.jpg",
        "content": "今天坚持健康饮食！",
        "fileIds": ["123", "456"],
        "likeCount": 10,
        "isLiked": false,
        "commentCount": 3,
        "comments": [
          {
            "id": 1,
            "userId": 2,
            "userName": "李四",
            "content": "加油！",
            "createTime": "2026-07-07T10:00:00"
          }
        ],
        "publishTime": "2026-07-07 09:30",
        "createTime": "2026-07-07T09:30:00"
      }
    ],
    "total": 100,
    "hasMore": true
  }
}
```

**数据模型**:
```typescript
interface FeedComment {
  id: number
  userId: number
  userName: string
  content: string
  createTime: string
}

interface FeedItem {
  id: number
  userId: number
  userName: string
  userAvatar: string | null
  content: string
  fileIds: string[]           // 附件ID数组
  likeCount: number           // 点赞数
  isLiked: boolean            // 当前用户是否已点赞
  commentCount: number        // 评论数
  comments: FeedComment[]     // 评论列表（返回前3条）
  publishTime: string         // 发布时间（格式化）
  createTime: string          // 创建时间
}

interface FeedListResult {
  list: FeedItem[]
  total: number
  hasMore: boolean
}
```

---

#### 2.6.2 发布动态

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/feed/publish` |
| **认证** | 需要 |
| **所属页面** | 轻友圈发布页 |
| **前端调用位置** | `src/api/qingyouquan/qingyouquan.ts` |

**请求参数** (`FeedPublishParam`):
```json
{
  "content": "今天坚持健康饮食！",
  "fileIds": ["123", "456"]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| content | String | 是* | 动态内容（content或fileIds至少一项） |
| fileIds | Array | 是* | 图片附件ID数组 |

**返回参数**:
```json
{
  "code": "200",
  "message": "发布成功",
  "data": {
    "feedId": 1
  }
}
```

---

#### 2.6.3 点赞/取消点赞

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/feed/{feedId}/like` |
| **认证** | 需要 |
| **所属页面** | 轻友圈列表页 |
| **前端调用位置** | `src/api/qingyouquan/qingyouquan.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| feedId | Long | 是 | 动态ID（路径参数） |

**返回参数**:
```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "isLiked": true,
    "likeCount": 11
  }
}
```

---

#### 2.6.4 添加评论

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/feed/{feedId}/comment` |
| **认证** | 需要 |
| **所属页面** | 轻友圈列表页 |
| **前端调用位置** | `src/api/qingyouquan/qingyouquan.ts` |

**请求参数**:
```json
{
  "content": "加油！"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| feedId | Long | 是 | 动态ID（路径参数） |
| content | String | 是 | 评论内容 |

**返回参数**:
```json
{
  "code": "200",
  "message": "评论成功",
  "data": {
    "commentId": 1
  }
}
```

---

### 2.7 附件模块

#### 2.7.1 上传附件

| 属性 | 值 |
|------|------|
| **路径** | `POST /api/attachment/upload` |
| **认证** | 需要 |
| **所属页面** | 轻友圈发布页、个人中心编辑页、AI营养师页 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**: multipart/form-data
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 上传的文件 |

**返回参数**:
```json
{
  "code": "200",
  "message": "上传成功",
  "data": {
    "id": "123456789",
    "fileName": "image.jpg",
    "fileUrl": "http://xxx/image.jpg"
  }
}
```

---

#### 2.7.2 获取附件URL

| 属性 | 值 |
|------|------|
| **路径** | `GET /api/attachment/{fileId}/url` |
| **认证** | 不需要 |
| **所属页面** | 所有需要展示图片的页面 |
| **前端调用位置** | `src/api/index.ts` |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | String | 是 | 文件ID（路径参数，雪花ID） |

**返回参数**:
```json
{
  "code": "200",
  "message": "success",
  "data": {
    "url": "http://xxx/image.jpg"
  }
}
```

---

## 三、数据库表设计建议

### 3.1 饮食记录表 (diet_record)

```sql
CREATE TABLE diet_record (
  id BIGINT PRIMARY KEY COMMENT '记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  record_date DATE NOT NULL COMMENT '记录日期',
  meal_type VARCHAR(20) NOT NULL COMMENT '餐段类型：breakfast/lunch/dinner/snack',
  remark VARCHAR(500) COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);
```

### 3.2 饮食项表 (diet_item)

```sql
CREATE TABLE diet_item (
  id BIGINT PRIMARY KEY COMMENT '饮食项ID',
  record_id BIGINT NOT NULL COMMENT '记录ID',
  food_name VARCHAR(100) NOT NULL COMMENT '食物名称',
  food_desc VARCHAR(500) COMMENT '食物描述',
  weight INT NOT NULL COMMENT '重量(g)',
  calories INT NOT NULL COMMENT '热量(kcal)',
  remark VARCHAR(500) COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
```

### 3.3 打卡表 (checkin_record)

```sql
CREATE TABLE checkin_record (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  checkin_date DATE NOT NULL COMMENT '打卡日期',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, checkin_date)
);
```

### 3.4 轻友圈动态表 (feed)

```sql
CREATE TABLE feed (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  content TEXT COMMENT '动态内容',
  file_ids VARCHAR(1000) COMMENT '附件ID数组JSON',
  like_count INT DEFAULT 0 COMMENT '点赞数',
  comment_count INT DEFAULT 0 COMMENT '评论数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
```

### 3.5 点赞表 (feed_like)

```sql
CREATE TABLE feed_like (
  id BIGINT PRIMARY KEY,
  feed_id BIGINT NOT NULL COMMENT '动态ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feed_user (feed_id, user_id)
);
```

### 3.6 评论表 (feed_comment)

```sql
CREATE TABLE feed_comment (
  id BIGINT PRIMARY KEY,
  feed_id BIGINT NOT NULL COMMENT '动态ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  content VARCHAR(500) NOT NULL COMMENT '评论内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
```

### 3.7 食物字典表 (food_dict)

```sql
CREATE TABLE food_dict (
  id BIGINT PRIMARY KEY,
  food_name VARCHAR(100) NOT NULL COMMENT '食物名称',
  category VARCHAR(50) COMMENT '分类',
  calories_per_100g INT COMMENT '每100g热量',
  protein_per_100g DECIMAL(10,2) COMMENT '每100g蛋白质',
  fat_per_100g DECIMAL(10,2) COMMENT '每100g脂肪',
  carbs_per_100g DECIMAL(10,2) COMMENT '每100g碳水',
  fiber_per_100g DECIMAL(10,2) COMMENT '每100g膳食纤维',
  edible_portion INT DEFAULT 100 COMMENT '可食用比例',
  data_source VARCHAR(100) COMMENT '数据来源',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
```

### 3.8 AI对话记录表 (nutritionist_chat)

```sql
CREATE TABLE nutritionist_chat (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
  content TEXT NOT NULL COMMENT '消息内容',
  file_ids VARCHAR(1000) COMMENT '附件ID数组JSON',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 四、接口开发优先级建议

| 优先级 | 模块 | 接口数量 | 说明 |
|--------|------|----------|------|
| P0（高） | 饮食记录 | 7 | 核心功能，首页依赖 |
| P0（高） | 统计分析 | 2 | 首页展示依赖 |
| P1（中） | 附件 | 2 | 图片上传展示基础 |
| P1（中） | 食物字典 | 1 | 搜索功能 |
| P2（低） | 打卡 | 3 | 辅助功能 |
| P2（低） | 轻友圈 | 4 | 社交功能 |
| P3（低） | AI营养师 | 2 | 需对接AI服务 |

---

## 五、注意事项

1. **JWT认证**: 所有标记"需要认证"的接口需在请求头携带 `Authorization: Bearer {token}`

2. **雪花ID**: 附件ID使用雪花算法生成，返回给前端为字符串类型

3. **餐段类型枚举**: `mealType` 取值：`breakfast`、`lunch`、`dinner`、`snack`

4. **逻辑删除**: 使用 `deleted` 字段，0表示正常，1表示已删除

5. **图片存储**: 使用附件表存储图片，业务表存储附件ID数组（JSON字符串）

6. **日期格式**: 所有日期统一使用 `YYYY-MM-DD` 格式，时间戳使用 `YYYY-MM-DDTHH:mm:ss`

7. **响应code**: 由于Jackson配置，code返回为字符串 `"200"`