# 营养管理系统

> 智能食物热量计算与营养摄入统计平台

## 项目介绍

营养管理系统是一款面向健康管理人群的智能营养助手小程序，通过 AI 技术帮助用户精准计算食物热量，记录饮食摄入，提供个性化营养建议。核心能力包括饮食记录与热量统计、AI 营养问答（ReAct Agent + RAG 知识库）、内容合规审核等，致力于为用户提供科学、便捷的健康管理体验。

系统采用 **Java + Python 双服务架构**：Java 后端（nutrition-server）负责业务数据管理、用户认证、OSS 存储、内容审核等；Python AI 服务（nutrition-AI）负责 RAG 知识库管理、向量检索、ReAct Agent 对话编排、AI 热量估算等。Java 通过 HTTP 调用 Python，Python 通过回调 Java 接口完成 MySQL 持久化。

**面向人群**：关注健康饮食、需要控制体重、追求营养均衡的用户群体

## 技术栈

### Java 后端技术（nutrition-server）

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.2 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存与会话管理 |
| JWT | 0.12.5 | 身份认证 |
| 阿里云 OSS | - | 对象存储 |
| 微信内容安全 API | v1/v2 | 内容审核 |
| Hutool | 5.8.25 | 工具库 |
| Sensitive-Word | 0.27.0 | 敏感词检测 |
| Spring-Retry | - | 远程调用重试 |

### Python AI 服务技术（nutrition-AI）

| 技术 | 说明 |
|------|------|
| Python 3.11+ | 运行环境 |
| FastAPI | Web 框架，暴露 RAG 和 AI 接口 |
| LangChain | 文档切片、向量嵌入、LLM 调用 |
| DashScope | 阿里云百炼 Embedding（text-embedding-v4）+ LLM（qwen-plus） |
| DashVector | 向量数据库，存储食物知识向量 |
| Redis | 会话缓存（active:session:{session_id}） |
| httpx | 异步 HTTP 客户端，回调 Java 接口 |
| Loguru | 日志框架 |

### 前端技术

| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 |
| TypeScript | 类型安全 |
| uni-app | 跨平台框架（微信小程序 + H5） |
| uView Plus | UI 组件库 |

## 特色功能

- **AI 营养助手**：基于 ReAct Agent 架构的智能问答，自主调度工具（热量计算、食物营养查询），SSE 流式输出
- **RAG 知识库**：食物营养数据向量化存储与语义检索，支持 PDF/DOCX/JSON/JSONL 等多格式文档上传
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
├── nutrition-server/           # Java 后端服务
│   ├── src/main/java/com/nutrition/
│   │   ├── controller/        # 控制层（AiController、DietRecordController、ChatSessionController等）
│   │   ├── client/            # Python AI 服务调用客户端（FastApiClient、FastApiProperties）
│   │   ├── service/           # 业务层
│   │   │   ├── impl/          # 服务实现
│   │   │   │   ├── AiFoodEstimateServiceImpl.java   # AI热量估算服务（调用Python）
│   │   │   │   ├── FoodNutritionServiceImpl.java    # 食物营养数据服务
│   │   │   │   └── ChatSessionServiceImpl.java      # AI会话管理服务
│   │   ├── mapper/            # 数据访问层（MyBatis-Plus）
│   │   ├── entity/            # 实体类（AiConfig、FoodNutrition、DietRecord、NutritionistChat等）
│   │   ├── vo/                # 视图对象（CalorieEstimateVO、ChatSessionVO等）
│   │   ├── dto/               # 数据传输对象
│   │   ├── config/            # 配置类（FastApiRestTemplateConfig、JwtAuthFilter等）
│   │   ├── enums/             # 枚举类（BizMsgEnum等）
│   │   ├── common/            # 公共组件（Result、BusinessException等）
│   │   ├── util/              # 工具类
│   │   └── task/              # 定时任务与启动Runner
│   ├── src/main/resources/
│   │   ├── mapper/            # MyBatis 映射文件
│   │   ├── db/                # 数据库脚本
│   │   └── application.yml    # 配置文件
│   └── pom.xml                # Maven 依赖
│
├── nutrition-AI/              # Python AI 服务
│   ├── main.py                # FastAPI 应用入口
│   ├── requirements.txt       # Python 依赖
│   ├── .env.example           # 环境变量示例
│   ├── config/                # 配置层
│   │   ├── settings.py        # 全局配置（从 .env 读取）
│   │   └── prompts/           # Prompt 模板
│   ├── constants/             # 全局常量（FileConstants、VectorConstants、AgentConstants等）
│   ├── models/                # 模型管理（全局单例）
│   │   ├── embedding_model.py # DashScope Embedding 单例（text-embedding-v4, 1024维）
│   │   └── llm_model.py       # ChatOpenAI 单例（配置从 Redis 加载，支持热更新）
│   ├── routers/               # FastAPI 路由层
│   │   ├── rag.py             # RAG 接口（文档上传/删除/检索）
│   │   └── ai.py              # AI 接口（热量估算）
│   ├── services/              # 服务层
│   │   ├── document_service.py    # 文档解析与切片（食物名称/营养文本分离入库）
│   │   ├── vector_service.py      # DashVector 向量存储与检索
│   │   ├── search_service.py      # 向量检索服务（distance阈值过滤）
│   │   ├── session_service.py     # AI会话服务（Redis缓存 + Java回调MySQL持久化）
│   │   ├── callback_service.py    # Java回调服务（文档入库状态回写）
│   │   ├── calorie_service.py     # AI热量估算服务
│   │   └── redis_service.py       # Redis操作封装
│   ├── Agent/                 # ReAct Agent 对话编排
│   │   ├── react_agent.py     # Agent 核心引擎（SSE流式输出、工具自主调用）
│   │   └── tools/             # Agent 工具集
│   │       ├── tool_registry.py       # 工具注册表（call_tool、build_tools_prompt）
│   │       ├── calorie_target_tool.py # 每日热量目标建议（Mifflin-St Jeor公式）
│   │       └── recommend_food_tool.py # 食物营养查询（RAG检索 + 语义重排）
│   ├── utils/                 # 工具层（auth、md5、response）
│   └── Test/                  # 集成测试
│       └── test_react_agent.py    # Agent 全链路测试脚本
│
├── nutrition-miniapp/         # uni-app 前端（微信小程序 + H5）
│   ├── src/
│   │   ├── pages/             # 页面组件
│   │   ├── api/               # API 接口封装
│   │   ├── components/        # 公共组件
│   │   ├── stores/            # 状态管理
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
- Python 3.11+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+
- Maven 3.8+
- 阿里云 DashScope API Key（Embedding + LLM）
- 阿里云 DashVector 实例（向量数据库）

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

#### Java 后端配置

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

# Python AI 服务调用配置
fastapi:
  base-url: http://localhost:8004
  api-secret-key: your-api-secret-key
  connect-timeout: 10
  read-timeout: 120  # 大JSONL文件上传需120秒
```

#### Python AI 服务配置

复制 `nutrition-AI/.env.example` 为 `nutrition-AI/.env`，填写以下配置：

```env
# DashScope（阿里云百炼）
DASHSCOPE_API_KEY=sk-your-dashscope-api-key

# DashVector（向量数据库）
DASHVECTOR_API_KEY=your-dashvector-api-key
DASHVECTOR_ENDPOINT=your-dashvector-endpoint
DASHVECTOR_COLLECTION_NAME=food-knowledge

# Java 后端回调地址
JAVA_BASE_URL=http://localhost:8088

# API 鉴权密钥（与 Java 端 fastapi.api-secret-key 保持一致）
API_SECRET_KEY=your-api-secret-key
```

### Java 后端启动

```bash
cd nutrition-server
mvn spring-boot:run
```

服务启动后访问：http://localhost:8088

### Python AI 服务启动

```bash
cd nutrition-AI
python -m venv .venv
.venv\Scripts\activate          # Windows
# source .venv/bin/activate     # Linux/macOS
pip install -r requirements.txt

# 在 MySQL 中配置 AI 模型参数到 Redis（ai_config 表），或使用默认配置
python main.py
```

Python AI 服务启动后访问：http://localhost:8004/docs（FastAPI Swagger）

### 前端启动（uni-app）

```bash
cd nutrition-miniapp
npm install
npm run dev:h5          # H5 开发模式
npm run build:h5        # H5 构建
npm run dev:mp-weixin   # 微信小程序开发模式
npm run build:mp-weixin # 微信小程序构建
```

## AI 服务架构

### 整体架构

```
                          ┌─── nutrition-server (Java :8088) ──────────────┐
                          │                                                 │
前端请求 → Java控制器 ────┤  FastApiClient ──HTTP──→ nutrition-AI (Python :8004)
                          │                           │                     │
                          │  ChatSessionController ←──┘ 回调Java接口        │
                          │  (会话CRUD、文档状态回写)                        │
                          └─────────────────────────────────────────────────┘
                                                      │
                          ┌─── nutrition-AI (Python :8004) ─────────────────┐
                          │                                                 │
                          │  routers/rag.py    → 文档上传/删除/检索          │
                          │  routers/ai.py     → 热量估算                    │
                          │  Agent/react_agent → ReAct对话编排(SSE流式)      │
                          │  services/         → 文档解析/向量存储/检索      │
                          │  models/           → Embedding + LLM 单例        │
                          │                                                 │
                          │  DashVector ← 向量存储与检索                     │
                          │  Redis      ← 会话缓存(12h TTL)                 │
                          │  Java回调   ← MySQL持久化(会话/文档状态)         │
                          └─────────────────────────────────────────────────┘
```

### Java → Python 调用链

1. **RAG 文档上传**：Java 接收文件 → 存 MySQL 记录(status=处理中) → HTTP 调 Python `/api/rag/document/upload` → Python 解析+切片+向量化+入 DashVector → 回调 Java `/api/rag/knowledge/callback` 更新 status=成功
2. **RAG 检索**：Java `/api/rag/search/query` → Python 向量检索 → 返回匹配结果
3. **AI 热量估算**：Java `/api/ai/estimate-calorie` → Python 调用 LLM → 返回热量数值
4. **AI 对话**：Python `Agent/react_agent.py` → ReAct 循环(思考→工具调用→观察→回答) → SSE 流式输出

### 数据来源

本系统食物营养数据来源于 **中国食物成分表** 开源项目：

- **项目地址**：https://github.com/Sanotsu/china-food-composition-data.git
- **数据内容**：包含常见食物的热量、蛋白质、脂肪、碳水化合物等营养成分信息（每100克）
- **数据格式**：JSON 格式，便于程序解析和导入
- **数据特点**：基于权威的中国食物成分表，覆盖日常常见食材，数据准确可靠
- **清洗后数据**：已处理缺失值、异常值，确保数据质量
                [食物数据库表数据](./nutrition-server/src/main/resources/foodData/food_nutrition.csv)
                 [RAG 知识库数据](./nutrition-server/src/main/resources/foodData/food_knowledge.jsonl)

## RAG 知识库模块

### 入库策略

食物数据采用 **embedding 文本与存储文本分离** 策略，最大化食物名称在向量检索中的权重：

| 字段 | 用途 | 说明 |
|------|------|------|
| `page_content` | Embedding 向量化 | 仅存纯食物名称（如"鸡蛋"），不加任何前缀或模板噪声 |
| `metadata.nutrition_text` | 检索返回展示 | 存完整营养描述（热量/蛋白质/脂肪/碳水），供 LLM 直接读取 |

普通文档（非食物）在 embedding 前添加 `document: ` 前缀以区分文档类型，检索返回时自动去除。

### 检索策略

- **向量模型**：DashScope text-embedding-v4，1024 维
- **距离度量**：DashVector cosine，返回 distance（1-similarity），**值越小越相似**
- **阈值过滤**：distance > 0.25 的结果视为低相似度，交由业务层裁决
- **食物查询语义重排**：对每个查询词返回 top3 候选，结合字面量匹配加分（完全匹配/包含匹配），解决"搜鸡蛋返回鸡蛋黄"等语义偏差问题

### 文档生命周期

```
上传文件 → Java 存 MySQL(delete_flag=1) → Python 向量化入 DashVector
    → 回调 Java 更新 status=成功, delete_flag=0, vector_store_id=xxx

删除文档 → Python 删 DashVector 向量 → Java 删 OSS 附件 → MySQL 逻辑删除(delete_flag=1)
```

## ReAct Agent 对话模块

### 架构设计

ReAct Agent 作为 AI 对话的编排引擎，采用 **思考-工具调用-观察** 循环模式：

```
用户提问 → 读取会话上下文 → 组装 Prompt(含工具描述) → 请求 LLM
    → LLM 返回思考+工具调用 → 执行工具 → 结果并入上下文 → 循环推理
    → LLM 返回最终回答 → SSE 流式输出(start/thought/tool_call/tool_result/answer/done)
```

### 已注册工具

| 工具名 | 功能 | 参数 |
|--------|------|------|
| `calorie_target_suggest` | 每日热量目标建议 | height, weight, gender, activity_level |
| `query_food_nutrition` | 食物营养查询（RAG检索） | food_names（食物名称列表） |

### 会话服务（session_service）

AI 对话会话采用 **Redis 缓存 + Java 回调 MySQL 持久化** 双层架构：

- **Redis 缓存**：key=`active:session:{session_id}`，12 小时 TTL，即时读写
- **MySQL 持久化**：Python 禁止直接操作 MySQL，全部通过 HTTP 回调 Java 接口（`/api/chat/session/*`）
- **消息角色**：user / ai_thought / tool_call / tool_result / ai_answer
- **落盘时机**：缓存过期前自动落盘 / 会话关闭时手动落盘

### Java 端会话接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/session/create` | POST | 创建新会话（JWT 白名单） |
| `/api/chat/session/flush` | POST | 批量落盘消息（JWT 白名单） |
| `/api/chat/session/{id}/history` | GET | 查询会话历史（JWT 白名单） |
| `/api/chat/session/list` | GET | 获取用户会话列表（需 JWT 认证） |

## 核心功能模块

### 1. AI 服务模块（Python nutrition-AI）

| 功能 | 说明 |
|------|------|
| ReAct Agent 对话 | 自主调度工具的智能问答，SSE 流式输出，支持多轮对话 |
| RAG 知识库 | 食物营养数据向量化存储与语义检索，支持多格式文档上传 |
| AI 热量估算 | 根据食物描述和重量估算总热量，支持 RAG 增强 |
| 会话管理 | Redis 缓存 + MySQL 持久化，12 小时热点缓存 |

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

- Java 后端 Swagger：http://localhost:8088/swagger-ui.html
- Python AI 服务 Swagger：http://localhost:8004/docs

详细 API 文档请参考：[docs/API.md](docs/API.md)

## 开发规范

### Java 后端规范

- 遵循阿里巴巴 Java 开发手册
- 使用 DTO/VO 模式进行数据传输
- 参数校验使用 `@Valid` + JSR-380 注解
- 异常处理使用统一的 `Result<T>` 返回格式
- 事务管理使用 `@Transactional`
- 调用 Python 服务使用专用 `fastApiRestTemplate`（独立超时配置）
- 远程调用重试使用 Spring-Retry（3 次，1s/2s/4s，仅网络异常）
- 错误消息提取到 `BizMsgEnum` 枚举，禁止硬编码

### Python AI 服务规范

- 分层架构：config → routers → services → models → utils
- **Python 禁止直接操作 MySQL**，所有持久化通过 HTTP 回调 Java 接口
- 敏感信息（API Key、端点）从 `.env` 环境变量读取，禁止硬编码
- 向量存储实例必须全局单例复用连接
- Embedding 模型从 `.env` 加载，LLM 模型从 Redis 加载（支持热更新）
- 所有接口返回统一 JSON 结构 `{code, msg, data}`
- 文档更新逻辑：先删旧向量再插新向量，确保一致性
- Agent 工具与 session_service 解耦，工具不引入会话相关代码
- 所有工具返回纯文本字符串，供 LLM 直接读取

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