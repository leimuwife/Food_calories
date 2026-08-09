from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator
from typing import List
import json


class Settings(BaseSettings):
    """
    全局配置类
    敏感配置优先级：系统环境变量 > .env文件 > 默认值
    """
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    # ==================== 服务配置 ====================
    server_port: int = 8000
    server_host: str = "0.0.0.0"

    # ==================== API鉴权配置 ====================
    api_secret_key: str = "your-secret-key-change-in-production"

    # ==================== Redis 缓存配置（食材基础热量缓存） ====================
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_password: str = ""
    redis_db: int = 0

    # ==================== 阿里云 DashVector 向量数据库配置 ====================
    vector_endpoint: str = ""
    vector_api_key: str = ""
    vector_collection_name: str = "food_knowledge_base"

    # ==================== 阿里云 DashScope Embedding 配置 ====================
    dashscope_api_key: str = ""
    embedding_model: str = "text-embedding-v4"

    # ==================== 文本分割配置 ====================
    chunk_size: int = 500
    chunk_overlap: int = 50
    separators: List[str] = [
        "\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", "，", ",", " ", ""
    ]


    # ==================== 大模型LLM请求超时（秒，Python侧控制，Redis中无此配置） ====================
    llm_timeout: int = 60

    # ==================== Java回调配置 ====================
    # Java后端基础地址（前缀/api由WebConfig统一添加）
    # 回调接口 = {java_base_url} + 接口路径：
    #   RAG入库回调：POST {java_base_url}/rag/knowledge/callback
    #   会话创建：   POST {java_base_url}/chat/session/create
    #   会话历史：   GET  {java_base_url}/chat/session/{sessionId}/history
    #   消息落盘：   POST {java_base_url}/chat/session/flush
    java_base_url: str = "http://localhost:8088/api"

    # RAG入库回调接口路径（相对java_base_url）
    java_rag_callback_path: str = "/rag/knowledge/callback"

    # ==================== 日志配置 ====================
    log_level: str = "INFO"

    @field_validator("separators", mode="before")
    @classmethod
    def parse_separators(cls, v):
        """将JSON字符串解析为列表"""
        if isinstance(v, str):
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                # 如果解析失败，尝试按逗号分割
                return [s.strip() for s in v.split(",")]
        return v


# 全局配置实例
settings = Settings()