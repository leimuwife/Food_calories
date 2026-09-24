# 健康分析模块设计

## 1. 背景与目标

现有营养小程序已具备饮食记录、热量统计和 AI 营养师对话能力，但缺少面向历史趋势的健康分析页面。本模块新增“健康分析”入口，聚合用户最近 7 天和 30 天每日热量数据，并允许用户选择目标类型，由 AI 生成差异化饮食分析报告。

目标如下：

1. 图表数据直接来自 MySQL 中的历史饮食记录，不经过 AI。
2. 提供最近 7 天和最近 30 天柱状图，柱顶标注每日热量。
3. 支持健身、减肥、正常饮食、增重四种目标类型。
4. AI 报告按目标类型生成差异化建议，并保存历史报告。
5. AI 服务连续失败 3 次后，返回同目标最近一条历史报告并标记为降级结果。
6. 所有目标类型、状态、重试次数和日期范围使用枚举或配置类管理，不硬编码业务字符串和魔法数字。

## 2. 范围

### 2.1 包含

- 首页“健康分析”快捷入口。
- 健康分析页面及响应式柱状图。
- 7 天、30 天每日热量查询接口。
- 四种用户目标枚举及请求参数校验。
- AI 智能分析报告生成、保存、查询和失败降级。
- 后端单元测试、接口验证和前端构建验证。

### 2.2 不包含

- 医护级诊断或医疗建议。
- 食物营养结构趋势、运动记录趋势等额外图表。
- 报告分享、导出和人工编辑。

## 3. 总体架构

```text
首页健康分析入口
        |
        v
健康分析页面
  |             |
  | 查询图表数据  | 选择目标并生成/加载报告
  v             v
Java 健康分析接口
  |             |
  | 聚合 MySQL   | 构造目标 Prompt
  v             v
diet_record      Python AI 健康报告接口
diet_item             |
                   返回文本
                      |
                      v
             health_analysis_report
```

图表链路不调用 AI；AI 链路只接收聚合后的热量摘要和目标类型。

## 4. 数据模型

### 4.1 新增表

```sql
CREATE TABLE `health_analysis_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报告主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goal_type` varchar(32) NOT NULL COMMENT '目标类型编码',
  `report_content` text NOT NULL COMMENT 'AI分析报告正文',
  `calorie_snapshot` text NOT NULL COMMENT '生成报告时的热量数据快照JSON',
  `last_7_avg` decimal(10,1) NOT NULL DEFAULT '0.0' COMMENT '最近7天日均热量',
  `last_30_avg` decimal(10,1) NOT NULL DEFAULT '0.0' COMMENT '最近30天日均热量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0正常 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_goal_create_time` (`user_id`,`goal_type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康分析报告表';
```

### 4.2 图表数据来源

- `diet_record` 提供用户、日期和记录归属。
- `diet_item` 提供每条饮食记录的热量。
- 按 `user_id + record_date` 聚合每日热量。
- 最近 7 天固定返回 7 项，最近 30 天固定返回 30 项。
- 无饮食记录的日期补 `0 kcal`，结果按日期升序排列。

## 5. 后端组件

### 5.1 枚举与配置

`HealthGoalTypeEnum`：

| 编码 | 中文名称 | 分析侧重点 |
| --- | --- | --- |
| `fitness` | 健身 | 蛋白质、训练恢复、避免极端热量缺口 |
| `weight_loss` | 减肥 | 温和热量缺口、饱腹感、蛋白质和膳食纤维 |
| `normal_diet` | 正常饮食 | 营养均衡、规律进餐、摄入稳定 |
| `weight_gain` | 增重 | 热量盈余、营养密度、多餐和力量训练 |

`HealthReportStatusEnum`：

- `current`：本次 AI 成功生成的报告。
- `fallback`：AI 失败后返回的同目标历史报告。

`HealthAnalysisConfigEnum`：

- 最近 7 天。
- 最近 30 天。
- AI 最大尝试次数 3。
- 重试基础等待时间。

### 5.2 类和职责

- `HealthAnalysisController`：对外提供热量趋势和报告接口。
- `HealthAnalysisService`：定义业务能力。
- `HealthAnalysisServiceImpl`：聚合数据、编排 AI、保存报告和降级查询。
- `HealthAnalysisReport`：报告持久化实体。
- `HealthAnalysisReportMapper`：报告表和热量聚合查询。
- `HealthAnalysisAiService`：封装 AI 报告调用，便于重试和单元测试。
- `HealthAnalysisPromptUtil`：生成四种目标对应的差异化 Prompt。
- `DailyCalorieDTO`：数据库聚合结果。
- `DailyCalorieVO`：单日图表数据。
- `HealthCalorieTrendVO`：7 天和 30 天趋势响应。
- `HealthAnalysisReportParam`：报告生成请求参数。
- `HealthAnalysisReportVO`：报告响应。

所有类、公开方法、字段和核心常量使用 `/** */` JavaDoc；业务目标、状态和配置禁止使用散落字符串或魔法数字。

## 6. API 设计

### 6.1 查询热量趋势

`GET /api/health-analysis/calories`

用户身份从 JWT 获取。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "last7Days": [
      { "date": "2026-09-18", "calories": 1820.5 }
    ],
    "last30Days": [
      { "date": "2026-08-26", "calories": 1750.0 }
    ]
  }
}
```

### 6.2 查询最近报告

`GET /api/health-analysis/report/latest?goalType=weight_loss`

- 仅查询 MySQL，不调用 AI。
- 没有报告时 `data` 为 `null`。
- 返回目标编码、中文名称、报告正文、报告状态和生成时间。

### 6.3 生成报告

`POST /api/health-analysis/report`

请求：

```json
{
  "goalType": "weight_loss"
}
```

处理流程：

1. 查询最近 7 天和 30 天热量数据。
2. 计算日均、最高日、最低日和记录天数。
3. 根据目标枚举生成差异化 Prompt。
4. 调用 AI，最多尝试 3 次，间隔递增。
5. 成功后保存报告并返回 `current` 状态。
6. 三次均失败时查询同目标最近报告。
7. 找到历史报告则返回 `fallback` 状态；未找到则返回“AI 分析服务暂不可用，请稍后再试”。

## 7. 前端设计

### 7.1 目录

- 页面：`nutrition-miniapp/src/pages/statistics/health-analysis/index.vue`
- 接口：`nutrition-miniapp/src/api/statistics/healthAnalysis.ts`
- 图表组件：`nutrition-miniapp/src/components/statistics/CalorieBarChart.vue`
- 类型定义：`nutrition-miniapp/src/api/types.ts`

### 7.2 首页入口

- 快捷入口扩展为三列布局。
- 新增 `healthAnalysis` 入口，名称为“健康分析”。
- “健康分析”和“小张营养师”位于同一行。
- 点击后跳转 `/pages/statistics/health-analysis/index`。

### 7.3 图表

- 不新增第三方图表库。
- 使用 CSS 柱状图，柱体高度由当日热量相对峰值计算。
- 每根柱顶部显示 kcal 数值。
- 7 天图在卡片宽度内铺开。
- 30 天图设置固定最小内容宽度，在卡片内横向滚动，避免数值重叠。
- 两个图表容器使用 `flex-wrap`：宽屏并排，窄屏自动上下排列。

### 7.4 报告交互

1. 进入页面默认选择“正常饮食”。
2. 加载该目标最近一次报告，不自动调用 AI。
3. 切换目标时加载对应目标最近报告。
4. 点击“生成分析报告”后调用生成接口。
5. `current` 状态展示新报告；`fallback` 状态展示提示“AI 服务暂不可用，当前为历史报告”。
6. 报告正文保留换行，显示生成时间、7 天平均和 30 天平均。

## 8. 错误处理

- 未登录：`401`。
- 非法目标类型：`400`，返回明确提示。
- 饮食数据查询失败：返回统计服务错误。
- AI 三次失败且无可降级报告：返回 AI 服务不可用提示。
- 报告保存失败：不返回未落库的新报告，按服务错误处理。
- 图表为空：所有日期显示 0，并展示空数据说明。

## 9. 测试方案

### 9.1 后端

- 四种目标编码和中文名称转换测试。
- 非法目标类型测试。
- 7 天和 30 天边界、补零、升序测试。
- 四种目标 Prompt 差异化测试。
- AI 第一次成功、第三次成功、三次失败测试。
- 同目标历史报告降级测试。
- 无历史报告时错误响应测试。
- 报告保存和最近报告查询测试。

### 9.2 前端

- 首页入口跳转测试。
- 路由注册和页面加载测试。
- 7 天、30 天图表数据渲染测试。
- 宽屏并排、窄屏换行和 30 天横向滚动测试。
- 目标切换、报告生成、降级提示测试。

### 9.3 命令验证

- `mvn -q test`
- `npm run build:h5`
- 启动后端实际调用三个健康分析接口。

## 10. 验收标准

1. 首页可进入健康分析页面。
2. 两个柱状图分别显示最近 7 天和最近 30 天每日热量及数值。
3. 图表在窄屏自动上下排列，30 天图可横向查看。
4. 四种目标均可选择和生成报告。
5. AI 成功后报告保存到 MySQL。
6. AI 三次失败后返回同目标最近报告并标记降级。
7. 没有历史报告时提示服务不可用。
8. 后端测试通过，前端全量构建通过。
