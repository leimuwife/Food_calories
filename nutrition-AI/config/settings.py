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

    # ==================== Java回调配置 ====================
    java_callback_url: str = "http://localhost:8088/api/rag/knowledge/callback"

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