"""模型管理层 - 统一管理向量模型和聊天模型的获取与复用"""

from models.embedding_model import get_embedding_model
from models.llm_model import get_llm_model

__all__ = ["get_embedding_model", "get_llm_model"]
