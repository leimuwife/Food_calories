"""
全局常量定义
统一管理系统中所有的静态常量、错误码、配置参数等

禁止在业务代码中直接硬编码常量值，必须引用此模块中的定义
"""

from typing import List


# ==================== API响应状态码 ====================

class ErrorCode:
    """API响应错误码"""
    # 成功
    SUCCESS = 200

    # 客户端错误 (4xx)
    BAD_REQUEST = 400          # 请求参数错误
    UNAUTHORIZED = 401         # 未授权
    FORBIDDEN = 403            # 禁止访问
    NOT_FOUND = 404            # 资源不存在

    # 文件处理错误 (1xxx)
    FILE_FORMAT_ERROR = 1001   # 文件格式错误
    FILE_PARSE_ERROR = 1002    # 文件解析失败
    FILE_EMPTY_ERROR = 1003    # 文件内容为空

    # 向量库错误 (2xxx)
    VECTOR_DB_ERROR = 2001     # 向量库操作失败
    VECTOR_TIMEOUT = 2002      # 向量库超时
    VECTOR_DIMENSION_ERROR = 2003  # 向量维度不匹配

    # Embedding错误 (3xxx)
    EMBEDDING_ERROR = 3001     # Embedding调用失败

    # 回调错误 (4xxx)
    CALLBACK_ERROR = 4001      # Java回调失败

    # 系统错误 (5xxx)
    INTERNAL_ERROR = 5000      # 内部服务器错误


# ==================== 通用常量 ====================

class CommonConstants:
    """通用常量"""
    # 默认返回码
    DEFAULT_SUCCESS_CODE = 200
    DEFAULT_ERROR_CODE = 500

    # 默认返回消息
    SUCCESS_MSG = "success"
    ERROR_MSG = "error"


# ==================== 文件相关常量 ====================

class FileConstants:
    """文件处理相关常量"""
    # 支持的文件格式
    SUPPORTED_FORMATS: List[str] = [".pdf", ".txt", ".md", ".docx", ".doc", ".json", ".jsonl"]

    # 文件大小限制（字节）
    MAX_FILE_SIZE = 50 * 1024 * 1024  # 50MB

    # 允许的文件扩展名
    ALLOWED_EXTENSIONS = {".pdf", ".txt", ".md", ".docx", ".doc", ".json"}


# ==================== 向量服务常量 ====================

class VectorConstants:
    """向量服务相关常量"""
    # 分批插入大小
    BATCH_SIZE = 50

    # Embedding批量大小
    EMBEDDING_BATCH_SIZE = 25

    # 检索距离阈值（DashVector cosine distance = 1 - similarity，值越小越相似，范围[0,2]）
    # distance > 此值的结果将被过滤（等价于 similarity < 0.75 的结果被丢弃）
    SEARCH_SCORE_THRESHOLD = 0.25

    # 删除分页大小（每页查询的向量数量）
    DELETE_PAGE_SIZE = 1000

    # 检索最大返回数量
    MAX_SEARCH_TOPK = 20

    # 检索最小返回数量
    MIN_SEARCH_TOPK = 1

    # 默认检索返回数量
    DEFAULT_SEARCH_TOPK = 5

    # 改动1: 检索对外入参topk固定约束区间（召回-重排-截取规则使用）
    RETRIEVE_MIN_TOPK = 8
    RETRIEVE_MAX_TOPK = 12

    # 向量维度枚举（text-embedding-v4 仅支持 1024 或 768）
    DIMENSION_1024 = 1024
    DIMENSION_768 = 768
    # 当前项目使用的向量维度（必须与DashVector集合维度、Embedding输出维度一致）
    VECTOR_DIMENSION = 1024

    # 文档内容前缀（标识知识库文档，帮助Embedding模型区分文档类型）
    DOCUMENT_PREFIX = "document: "


# ==================== Redis相关常量 ====================

class RedisConstants:
    """Redis缓存相关常量"""
    # 食材基础热量缓存Hash Key（与Java端 FoodNutritionCacheRunner 保持一致）
    FOOD_NUTRITION_HASH_KEY = "food:nutrition"

    # 当前启用的AI模型配置缓存Key（与Java端 AiConfigCacheRunner 保持一致）
    # String结构，value为精简配置JSON（含modelName/apiUrl/apiKey/temperature/maxTokens/systemPrompt）
    AI_CONFIG_ENABLED_KEY = "ai:config:enabled"

    # AI会话消息缓存Key前缀（Redis List，元素为JSON结构化消息 {"role":..., "content":...}）
    SESSION_CACHE_PREFIX = "active:session:"

    # AI会话消息缓存过期时间（秒）：12小时
    SESSION_CACHE_TTL_SECONDS = 12 * 3600

    # 获取会话历史时最多返回的消息条数（约10轮交互，每轮≈2条）
    SESSION_HISTORY_MESSAGE_COUNT = 20

    # 缓存临近过期提前落盘阈值（秒）：剩余TTL低于该值触发批量落盘，防止缓存过期丢数据
    SESSION_TTL_SWEEP_THRESHOLD_SECONDS = 3600

    # 后台落盘扫描间隔（秒）
    SESSION_SWEEP_INTERVAL_SECONDS = 300


# ==================== AI聊天会话常量 ====================

class ChatConstants:
    """AI聊天会话相关常量"""
    # 合法角色枚举（与Java端 ChatRoleEnum 一一对应）
    VALID_ROLES = ("user", "ai_thought", "tool_call", "tool_result", "ai_answer")


# ==================== Agent工具常量 ====================

class AgentConstants:
    """ReAct Agent工具相关常量"""

    # ---- 活动系数映射（Mifflin-St Jeor公式配套） ----
    ACTIVITY_FACTORS = {
        "久坐办公": 1.2,
        "轻度活动": 1.375,
        "中度活动": 1.55,
        "高强度体力运动": 1.725,
    }
    # 默认活动等级
    DEFAULT_ACTIVITY_LEVEL = "久坐办公"
    # 用户未提供年龄时的默认估算年龄
    DEFAULT_AGE = 25
    # 温和减脂热量缺口区间（大卡/天）
    MILD_DEFICIT_MIN = 300
    MILD_DEFICIT_MAX = 500
    # 高强度减脂热量缺口（大卡/天）
    AGGRESSIVE_DEFICIT = 600
    # 减脂摄入量修正系数：在缺口计算基础上再下降20%，使减脂区间整体摄入更低
    MILD_DEFICIT_FACTOR = 0.8
    AGGRESSIVE_DEFICIT_FACTOR = 0.8

    # ---- 工具异常提示文本 ----
    MSG_MISSING_PARAMS = "请向用户收集以下必要信息后再次调用：身高、体重、性别。"
    MSG_NO_FOOD_MATCH = "当前知识库暂时没有匹配该条件的食物，请建议用户调整筛选条件或等待知识库更新。"
    MSG_RAG_ERROR = "食物知识库检索服务暂时不可用，请稍后重试或基于已有知识回答用户。"
    MSG_INVALID_ACTIVITY = "未识别的活动量等级，已按默认久坐办公计算。"

    # ---- ReAct Agent循环相关 ----
    # 最大工具调用轮次（防止无限循环调用工具）
    MAX_AGENT_ITERATIONS = 5
    # 工具调用错误兜底提示文本
    MSG_AGENT_ERROR = "抱歉，我在处理过程中遇到了问题，请稍后重试。"
    # 模型超时错误兜底提示文本
    MSG_MODEL_TIMEOUT = "模型响应超时，请稍后重试。"

    # ---- 食物检索相关 ----
    # 检索食物时使用的topk（召回更多结果便于取最佳匹配）
    FOOD_SEARCH_TOPK = 12


# ==================== 安全相关常量 ====================

class SecurityConstants:
    """安全相关常量"""
    # API鉴权Header
    AUTH_HEADER = "Authorization"
    AUTH_PREFIX = "Bearer "

    # MD5相关
    MD5_ALGORITHM = "MD5"
    MD5_LENGTH = 32


# ==================== 日志相关常量 ====================

class LogConstants:
    """日志相关常量"""
    # 日志格式
    LOG_SEPARATOR = "=" * 60

    # 耗时单位转换（秒转毫秒）
    MS_MULTIPLIER = 1000.0

    # 耗时日志格式
    COST_LOG_FORMAT = "{:.1f}ms"
