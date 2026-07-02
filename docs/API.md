# 营养助手 - API 接口文档

## 基本信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Bearer Token（登录成功后在 Header 中携带）
- **请求头**: `Authorization: Bearer {token}`
- **Content-Type**: `application/json`（除特殊标注外）
- **统一响应格式**:

```json
{
  "code": 200,
  "message": "成功",
  "data": { ... }
}
```

错误响应:
```json
{
  "code": 400/401/500,
  "message": "错误描述",
  "data": null
}
```

---

## 一、用户认证模块

### 1.1 账号密码登录

```
POST /api/auth/login
```

**请求体**:
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOi...",
    "user": {
      "id": 1,
      "nickname": "新用户",
      "avatar": null,
      "email": null
    }
  }
}
```

### 1.2 账号注册

```
POST /api/auth/register
```

**请求体**:
```json
{
  "username": "newuser",
  "password": "123456",
  "nickname": "小明"
}
```

**响应**: 返回 token 和用户信息（结构同上）

### 1.3 微信一键登录

```
POST /api/auth/wx-login
```

**请求体**:
```json
{
  "code": "wx_login_code_from_wx"
}
```

**说明**: `code` 由 `wx.login()` 获取，后端通过微信 API 换取 openid。新用户自动注册。

---

## 二、用户信息模块（需认证）

### 2.1 获取个人信息

```
GET /api/user/profile
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "nickname": "小明",
    "avatar": null,
    "email": "test@example.com",
    "dailyCalorieGoal": 2000,
    "dailyProteinGoal": 60,
    "dailyFatGoal": 55,
    "dailyCarbsGoal": 250
  }
}
```

### 2.2 更新个人信息

```
PUT /api/user/profile
```

**请求体**:
```json
{
  "nickname": "小明(减肥中)",
  "avatar": "https://example.com/avatar.jpg",
  "email": "new@example.com"
}
```

### 2.3 更新营养目标

```
PUT /api/user/goal
```

**请求体**:
```json
{
  "goals": {
    "dailyCalorieGoal": 1800,
    "dailyProteinGoal": 80,
    "dailyFatGoal": 45,
    "dailyCarbsGoal": 220
  }
}
```

---

## 三、食物数据模块

### 3.1 搜索食物

```
GET /api/food/search?keyword=鸡蛋&category=肉蛋奶
```

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 搜索关键词，支持模糊匹配 |
| category | string | 否 | 食物分类筛选 |

**响应**:
```json
{
  "code": 200,
  "data": {
    "total": 5,
    "list": [
      {
        "id": 31,
        "foodName": "鸡蛋(煮)",
        "category": "肉蛋奶",
        "caloriesPer100g": 144,
        "proteinPer100g": 13.3,
        "fatPer100g": 8.8,
        "carbsPer100g": 2.8,
        "fiberPer100g": 0.0,
        "ediblePortion": 100.0,
        "dataSource": "中国食物成分表"
      }
    ]
  }
}
```

### 3.2 食物详情

```
GET /api/food/{id}
```

### 3.3 食物分类列表

```
GET /api/food/categories
```

**响应**:
```json
{
  "code": 200,
  "data": ["主食", "肉蛋奶", "蔬菜", "水果", "零食", "饮品", "调味品", "其他"]
}
```

---

## 四、饮食记录模块（需认证）

### 4.1 新增饮食记录

```
POST /api/diet/record
```

**请求体**:
```json
{
  "recordDate": "2025-01-15",
  "mealType": "breakfast",
  "remark": "今天早餐吃得不错",
  "items": [
    {
      "foodId": 1,
      "weight": 200
    },
    {
      "foodId": 31,
      "weight": 100
    }
  ]
}
```

**参数说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recordDate | string | 是 | 日期，格式 yyyy-MM-dd |
| mealType | string | 是 | 餐次: breakfast/lunch/dinner/snack |
| remark | string | 否 | 备注 |
| items[].foodId | long | 是 | 食物ID |
| items[].weight | int | 是 | 食用重量(g) |

**响应**:
```json
{
  "code": 200,
  "message": "添加成功",
  "data": { "recordId": 1 }
}
```

### 4.2 按日期查询记录

```
GET /api/diet/record?date=2025-01-15
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "summary": {
      "totalCalories": 500,
      "totalProtein": 28.0,
      "totalFat": 10.5,
      "totalCarbs": 65.0,
      "breakfastCalories": 500,
      "lunchCalories": 0,
      "dinnerCalories": 0,
      "snackCalories": 0
    },
    "records": [
      {
        "id": 1,
        "mealType": "breakfast",
        "remark": "今天早餐吃得不错",
        "items": [
          {
            "id": 1,
            "foodId": 1,
            "foodName": "白米饭",
            "weight": 200,
            "calories": 232,
            "protein": 5.2,
            "fat": 0.6,
            "carbs": 51.8
          }
        ]
      }
    ]
  }
}
```

### 4.3 删除饮食记录

```
DELETE /api/diet/record/{id}
```

### 4.4 更新明细重量

```
PUT /api/diet/item/{id}
```

**请求体**:
```json
{
  "weight": 150
}
```

### 4.5 删除明细

```
DELETE /api/diet/item/{id}
```

### 4.6 复制历史餐食到指定日期

```
POST /api/diet/record/{id}/copy
```

**请求体**:
```json
{
  "targetDate": "2025-01-16"
}
```

---

## 五、统计分析模块（需认证）

### 5.1 每日营养汇总

```
GET /api/statistics/daily?date=2025-01-15
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalCalories": 1800,
    "totalProtein": 75.0,
    "totalFat": 50.0,
    "totalCarbs": 230.0,
    "calorieGoal": 2000,
    "caloriePercent": 90.0,
    "proteinPercent": 80.0,
    "fatPercent": 70.0,
    "carbsPercent": 85.0,
    "proteinEnergyRatio": 16.7,
    "fatEnergyRatio": 25.0,
    "carbsEnergyRatio": 51.1,
    "mealBreakdown": {
      "breakfast": 500,
      "lunch": 700,
      "dinner": 500,
      "snack": 100
    }
  }
}
```

### 5.2 月度营养统计

```
GET /api/statistics/monthly?year=2025&month=1
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "avgDailyCalories": 1750,
    "avgProtein": 70.0,
    "avgFat": 48.0,
    "avgCarbs": 220.0,
    "avgProteinEnergyRatio": 16.0,
    "avgFatEnergyRatio": 24.7,
    "avgCarbsEnergyRatio": 50.3,
    "dailyTrend": [
      { "date": "2025-01-01", "calories": 1800, "protein": 75, "fat": 50, "carbs": 230 },
      { "date": "2025-01-02", "calories": 1700, "protein": 65, "fat": 45, "carbs": 215 }
    ],
    "topFoods": [
      { "foodName": "鸡胸肉", "count": 15 },
      { "foodName": "西兰花", "count": 12 }
    ]
  }
}
```

### 5.3 导出 CSV

```
GET /api/statistics/export?startDate=2025-01-01&endDate=2025-01-31
```

**响应**: 返回 CSV 文本内容，前端可保存为 .csv 文件。

---

## 状态码说明

| 状态码 | 含义 |
|--------|------|
| 200 | 请求成功 |
| 400 | 参数校验失败 |
| 401 | 未认证 / Token 过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 餐次类型枚举

| 值 | 含义 |
|----|------|
| breakfast | 早餐 |
| lunch | 午餐 |
| dinner | 晚餐 |
| snack | 加餐 |
