"""向量模型（Embedding）获取 - 全局单例复用

配置来源：.env 文件 → settings.py 读取（静态配置，启动时确定）
模型：DashScope text-embedding-v4（1024维）

使用方式：
    from models import get_embedding_model
    embeddings = get_embedding_model()
    vectors = embeddings.embed_documents(["牛蛙"])
"""
from typing import Optional

from loguru import logger
from langchain_community.embeddings.dashscope import DashScopeEmbeddings

from config.settings import settings


__embedding_instance: Optional[DashScopeEmbeddings] = None


def get_embedding_model() -> DashScopeEmbeddings:
    """
    获取DashScope Embedding模型单例

    配置从 .env → settings 读取：
    - embedding_model: 模型名称（默认 text-embedding-v4）
    - dashscope_api_key: DashScope API密钥（需 sk- 前缀）

    Returns:
        DashScopeEmbeddings 实例

    Raises:
        RuntimeError: API密钥未配置
    """
    global __embedding_instance
    if __embedding_instance is None:
        api_key = settings.dashscope_api_key.strip()
        if not api_key:
            logger.error("DashScope API密钥未配置，请检查 .env 中 DASHSCOPE_API_KEY")
            raise RuntimeError("DashScope API密钥未配置，请检查 .env 中 DASHSCOPE_API_KEY")

        __embedding_instance = DashScopeEmbeddings(
            model=settings.embedding_model,
            dashscope_api_key=api_key
        )
        logger.info("Embedding模型初始化完成: model={}", settings.embedding_model)

    return __embedding_instance
