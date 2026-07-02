# 营养助手 - 项目部署文档

## 一、环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 后端编译运行 |
| Maven | 3.8+ | 后端构建 |
| MySQL | 8.0+ | 数据库，InnoDB引擎，utf8mb4字符集 |
| Node.js | 18+ | 前端构建（Vite依赖） |
| HBuilder X | 最新版 | uni-app 开发工具 |
| 微信开发者工具 | 最新稳定版 | 小程序调试预览 |

---

## 二、数据库初始化

### Step 1: 启动 MySQL 并登录

```bash
mysql -u root -p
```

### Step 2: 执行初始化脚本

```bash
source /path/to/nutrition-server/src/main/resources/db/init.sql
```

或通过 MySQL 客户端导入 `init.sql` 文件。

### Step 3: 验证数据

```sql
USE nutrition_db;
SELECT COUNT(*) FROM food_dict;  -- 应显示 200
SELECT * FROM food_dict LIMIT 5;
```

---

## 三、后端部署

### 3.1 配置 application.yml

编辑 `nutrition-server/src/main/resources/application.yml`，修改以下占位符：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nutrition_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: your_mysql_username    # ← 改为你的MySQL用户名
    password: your_mysql_password    # ← 改为你的MySQL密码

jwt:
  secret: your-jwt-secret-key-change-in-production  # ← 改为随机字符串

# 微信小程序配置（可选）
wechat:
  appid: your_wechat_appid           # ← 微信小程序AppID
  secret: your_wechat_appsecret      # ← 微信小程序AppSecret

# 第三方营养API（可选，用于扩展食物搜索）
nutrition:
  usda-api-key: your_usda_api_key    # ← USDA FoodData Central API Key
```

### 3.2 启动后端

```bash
cd nutrition-server

# 方式一：Maven 启动
mvn clean package -DskipTests
java -jar target/nutrition-server-1.0.0.jar

# 方式二：IDE 直接运行 NutritionApplication.java
```

### 3.3 验证

```bash
# 测试搜索接口
curl http://localhost:8080/api/food/search?keyword=鸡蛋

# 测试注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","nickname":"测试用户"}'
```

---

## 四、前端部署

### 4.1 安装依赖

```bash
cd nutrition-miniapp
npm install
```

### 4.2 配置 API 地址

编辑 `src/api/index.ts`，修改 `BASE_URL`：

```typescript
const BASE_URL = 'http://localhost:8080/api'  // 开发环境
// 生产环境改为你的服务器地址
```

### 4.3 使用 HBuilder X 运行

1. 打开 HBuilder X
2. 文件 → 导入 → 从本地目录导入 → 选择 `nutrition-miniapp` 目录
3. 运行 → 运行到小程序模拟器 → 微信开发者工具
4. 首次运行会自动编译，等待完成后自动打开微信开发者工具

### 4.4 微信开发者工具配置

1. 在微信开发者工具中导入项目
2. 项目目录选择 `nutrition-miniapp/dist/dev/mp-weixin`
3. AppID 填入你的小程序 AppID（测试可用测试号）
4. 在「详情 → 本地设置」中勾选「不校验合法域名」

### 4.5 预览与真机调试

- **预览**: 点击工具栏「预览」生成二维码，手机扫码体验
- **真机调试**: 点击「真机调试」，手机扫码后可在开发者工具中实时调试
- **上传**: 开发完成后点击「上传」，填写版本号后提交审核

---

## 五、第三方营养 API Key 申请方式

### 5.1 USDA FoodData Central API

1. 访问 https://fdc.nal.usda.gov/api-guide.html
2. 点击「Sign Up」注册账号
3. 登录后在 API Guide 页面获取 API Key
4. 将 Key 填入 `application.yml` 的 `nutrition.usda-api-key`

### 5.2 国内替代方案

- 中国营养学会开放平台（如有）
- 自建食物数据库（项目已内置200+常见食物）

---

## 六、常见问题

### Q: 前端请求报 404？
A: 确认后端已启动，且 `src/api/index.ts` 中的 `BASE_URL` 正确。

### Q: 小程序请求报「不在以下 request 合法域名列表中」？
A: 开发阶段在微信开发者工具「详情 → 本地设置」勾选「不校验合法域名」。

### Q: 数据库连接失败？
A: 确认 MySQL 已启动，用户名密码正确，数据库 `nutrition_db` 已创建。

### Q: JWT 解析失败？
A: 确认 `application.yml` 中 `jwt.secret` 与生成 token 时一致。
