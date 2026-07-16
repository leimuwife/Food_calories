# 营养管理系统

> 智能食物热量计算与营养摄入统计平台

## 项目介绍

营养管理系统是一款面向健康管理人群的智能营养助手小程序，通过 AI 技术帮助用户精准计算食物热量，记录饮食摄入，提供个性化营养建议。核心能力包括饮食记录与热量统计、AI 营养问答、内容合规审核等，致力于为用户提供科学、便捷的健康管理体验。

**面向人群**：关注健康饮食、需要控制体重、追求营养均衡的用户群体

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.2 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存与会话管理 |
| JWT | 0.12.5 | 身份认证 |
| 阿里云 OSS | - | 对象存储 |
| 阿里云百炼 API | - | AI 大模型服务 |
| DashVector | - | 向量数据库 |
| 微信内容安全 API | v1/v2 | 内容审核 |
| Hutool | 5.8.25 | 工具库 |
| Sensitive-Word | 0.27.0 | 敏感词检测 |

### 前端技术

| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 |
| TypeScript | 类型安全 |
| uni-app | 跨平台框架（微信小程序 + H5） |
| uView Plus | UI 组件库 |

## 特色功能

- **AI 营养助手**：基于 RAG 知识库的智能问答，提供专业营养建议
- **AI 热量估算**：支持食物描述 + 重量双重参数输入，精准估算热量
- **饮食记录**：记录早中晚餐及加餐，统计每日热量摄入
- **内容审核**：图片与文本安全审核，确保社区内容合规
- **社区互动**：动态发布、点赞、评论
- **打卡功能**：每日健康打卡记录
- **用户管理**：个人信息管理、问题反馈

## 功能截图

![首页](./docs/image/shouye.png)
![早餐页面](./docs/image/zaocan.png)
![打卡页面](./docs/image/daka.png)
![轻友圈](./docs/image/qingyouquan.png)
![发布动态页面](./docs/image/fabudongtai.png)
![添加早餐](./docs/image/tianjiazaocan.png)
![个人中心](./docs/image/gerenzhongxin.png)

## 核心项目结构

```
nutrition-all/
├── nutrition-server/           # 后端服务
│   ├── src/main/java/com/nutrition/
│   │   ├── controller/        # 控制层（AiController、DietRecordController等）
│   │   ├── service/           # 业务层
│   │   │   ├── impl/          # 服务实现
│   │   │   │   ├── AiFoodEstimateServiceImpl.java   # AI热量估算服务
│   │   │   │   ├── AiModelServiceImpl.java          # AI对话服务
│   │   │   │   ├── FoodNutritionServiceImpl.java    # 食物营养数据服务
│   │   │   │   └── FoodKnowledgeRetrievalServiceImpl.java  # RAG检索服务
│   │   │   ├── AiFoodEstimateService.java           # AI热量估算接口
│   │   │   ├── AiModelService.java                  # AI对话接口
│   │   │   ├── FoodNutritionService.java            # 食物营养数据接口
│   │   │   └── FoodKnowledgeRetrievalService.java   # RAG检索接口
│   │   ├── mapper/            # 数据访问层（MyBatis-Plus）
│   │   ├── entity/            # 实体类（AiConfig、FoodNutrition、DietRecord等）
│   │   ├── vo/                # 视图对象（CalorieEstimateVO、ChatResponseVO等）
│   │   ├── dto/               # 数据传输对象（NutritionDTO、KnowledgeDTO等）
│   │   ├── config/            # 配置类
│   │   │   ├── AiEstimatePromptConfig.java          # AI估算Prompt配置
│   │   │   ├── VectorRetrievalProperties.java       # 向量检索配置
│   │   │   └── DashVectorClientConfig.java          # DashVector客户端配置
│   │   ├── enums/             # 枚举类（BizMsgEnum等）
│   │   ├── common/            # 公共组件（Result、BusinessException等）
│   │   ├── util/              # 工具类（AesUtil等）
│   │   └── task/              # 定时任务与启动Runner
│   │       ├── FoodNutritionCacheRunner.java        # 食物营养缓存预热
│   │       └── FoodKnowledgeEmbeddingRunner.java    # 知识向量批量入库
│   ├── src/main/resources/
│   │   ├── mapper/            # MyBatis 映射文件
│   │   ├── db/                # 数据库脚本
│   │   └── application.yml    # 配置文件
│   └── pom.xml                # Maven 依赖
│
├── nutrition-miniapp/         # uni-app 前端（微信小程序 + H5）
│   ├── src/
│   │   ├── pages/             # 页面组件
│   │   │   ├── shouye/        # 首页
│   │   │   ├── add/           # 饮食记录（早餐、午餐、晚餐、夜宵）
│   │   │   ├── checkin/       # 打卡页面
│   │   │   ├── feed/          # 社区动态
│   │   │   └── user/          # 个人中心
│   │   ├── api/               # API 接口封装
│   │   ├── components/        # 公共组件
│   │   ├── stores/            # 状态管理
│   │   ├── styles/            # 样式文件
│   │   └── utils/             # 工具函数
│   └── package.json           # 前端依赖
│
└── docs/                      # 文档
    ├── API.md                 # API 文档
    └── DEPLOY.md              # 部署文档
```

## 快速启动

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+
- Maven 3.8+

### 数据库初始化

1. 创建数据库：
```sql
CREATE DATABASE nutrition_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```bash
mysql -u username -p nutrition_db < nutrition-server/src/main/resources/db/nutrition_db.sql
```

### 配置文件修改

修改 `nutrition-server/src/main/resources/application.yml`：

```yaml
server:
  port: 8088

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nutrition_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: your_username
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

# 微信小程序配置（通过环境变量设置）
wx:
  mini:
    appid: ${WX_MINI_APPID}
    appsecret: ${WX_MINI_APPSECRET}
    audit-version: 1

# 阿里云 OSS 配置
oss:
  endpoint: your-oss-endpoint
  bucket-name: your-bucket-name
  access-key-id: your-access-key-id
  access-key-secret: your-access-key-secret

# AI 配置
ai:
  estimate:
    system-prompt: 你是一个专业的热量计算助手...
    user-prompt-template: 请根据以下食物描述和重量估算总热量...

# 向量检索配置
vector:
  retrieval:
    endpoint: your-dashvector-endpoint
    api-key: your-dashvector-api-key
    collection-name: food-knowledge
```

### 后端启动

```bash
cd nutrition-server
mvn spring-boot:run
```

服务启动后访问：http://localhost:8088

### 前端启动（uni-app）

```bash
cd nutrition-miniapp
npm install
npm run dev:h5          # H5 开发模式
npm run build:h5        # H5 构建
npm run dev:mp-weixin   # 微信小程序开发模式
npm run build:mp-weixin # 微信小程序构建
```

## AI 热量估算流程

### 整体架构

```
用户输入 → 前端请求 → AI控制器 → 热量估算服务 → [缓存 → 向量检索 → 大模型调用] → 返回结果
```

### 详细流程

1. **参数接收**：前端传入 `foodDesc`（食物描述）和可选的 `weight`（重量，单位：克）

2. **缓存查询**：首先查询 Redis 缓存，若命中则直接返回结果

3. **限流检查**：检查请求频率，防止恶意调用

4. **并行数据检索**：
   - **食物营养数据检索**：从 Redis 缓存（预热自 MySQL）中查询食材营养信息（每100克热量、蛋白质、脂肪、碳水化合物）
   - **RAG 知识库检索**：通过 DashVector 向量数据库进行语义检索，获取相关食物知识

5. **大模型调用**：
   - 使用 `AiEstimatePromptConfig` 配置的系统 Prompt 和用户 Prompt 模板
   - 将食物描述、重量、营养数据、知识库信息拼接成完整 Prompt
   - 调用阿里云百炼大模型（qwen-plus）进行热量估算

6. **结果解析与验证**：
   - 提取模型返回的数值
   - 验证数值范围（0 < calorie < 10000 kcal）

7. **降级策略**：若大模型调用失败或验证不通过，依次尝试：
   - 使用 Redis 缓存的营养数据计算
   - 使用 RAG 知识库信息计算
   - 使用预定义的食物热量配方计算

8. **缓存写入**：将有效结果写入 Redis 缓存（24小时过期）

### 数据来源

本系统食物营养数据来源于 **中国食物成分表** 开源项目：

- **项目地址**：https://github.com/Sanotsu/china-food-composition-data.git
- **数据内容**：包含常见食物的热量、蛋白质、脂肪、碳水化合物等营养成分信息（每100克）
- **数据格式**：JSON 格式，便于程序解析和导入
- **数据特点**：基于权威的中国食物成分表，覆盖日常常见食材，数据准确可靠
- **清洗后数据**：已处理缺失值、异常值，确保数据质量
                [食物数据库表数据](./nutrition-server/src/main/resources/foodData/food_nutrition.csv)
                 [RAG 知识库数据](./nutrition-server/src/main/resources/foodData/food_knowledge.jsonl)
### 数据加载流程

1. **数据导入**：启动时通过 `FoodNutritionCacheRunner` 将食物营养数据全量加载到 Redis 缓存
2. **缓存结构**：使用 Redis Hash 结构，Key 为 `food:nutrition`，Field 为食物名称，Value 为营养数据 JSON
3. **缓存有效期**：7天自动过期，支持缓存预热更新
4. **降级查询**：Redis 查询失败时直接查询 MySQL 数据库

## 核心功能模块

### 1. AI 服务模块

| 功能 | 说明 |
|------|------|
| AI 热量估算 | 根据食物描述和重量估算总热量，支持 RAG 增强 |
| AI 营养师对话 | 基于大模型的营养健康问答，支持多轮对话 |
| 向量知识库 | 食物营养知识向量化存储与语义检索 |

### 2. 饮食记录与热量统计

- 记录早、中、晚餐及加餐
- AI 估算食物热量（支持描述 + 重量参数）
- 每日热量摄入统计
- 食物列表管理

### 3. 内容审核

- 图片安全审核（微信内容安全 API）
- 文本敏感词检测
- 审核记录管理
- v1/v2 版本配置化切换

### 4. 用户管理

- 微信小程序登录
- 个人信息管理
- 头像上传与审核
- 问题反馈

### 5. 社区互动

- 动态发布
- 点赞与评论
- 动态列表展示

## API 文档

启动后端服务后访问：http://localhost:8088/swagger-ui.html

详细 API 文档请参考：[docs/API.md](docs/API.md)

## 开发规范

### 后端规范

- 遵循阿里巴巴 Java 开发手册
- 使用 DTO/VO 模式进行数据传输
- 参数校验使用 `@Valid` + JSR-380 注解
- 异常处理使用统一的 `Result<T>` 返回格式
- 事务管理使用 `@Transactional`
- AI 服务使用长超时 `aiRestTemplate`
- 向量库操作使用官方 SDK，禁止手写 URL 拼接

### 前端规范

- TypeScript 严格模式
- 组件化开发
- 统一的 API 接口封装
- 响应式状态管理

## 部署说明

详细部署指南请参考：[docs/DEPLOY.md](docs/DEPLOY.md)

## 许可证

本项目开源协议为 [MIT License](./LICENSE)，详情见仓库根目录 LICENSE 文件。

## 贡献

欢迎提交 Issue 和 Pull Request！