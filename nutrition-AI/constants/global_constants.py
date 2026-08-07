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
    SUPPORTED_FORMATS: List[str] = [".pdf", ".txt", ".md", ".docx", ".doc", ".json"]

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

    # 检索相似度阈值（低于此值的结果将被过滤）
    SEARCH_SCORE_THRESHOLD = 0.75

    # 删除分页大小（每页查询的向量数量）
    DELETE_PAGE_SIZE = 1000

    # 检索最大返回数量
    MAX_SEARCH_TOPK = 20

    # 检索最小返回数量
    MIN_SEARCH_TOPK = 1

    # 默认检索返回数量
    DEFAULT_SEARCH_TOPK = 5

    # 向量维度枚举（text-embedding-v4 仅支持 1024 或 768）
    DIMENSION_1024 = 1024
    DIMENSION_768 = 768
    # 当前项目使用的向量维度（必须与DashVector集合维度、Embedding输出维度一致）
    VECTOR_DIMENSION = 1024


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
