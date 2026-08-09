"""聊天模型（LLM）获取 - 全局单例复用

配置来源：Redis（Java端 AiConfigCacheRunner 预热写入，动态可变）
模型：OpenAI兼容接口（ChatOpenAI，支持通义千问等兼容服务）

使用方式：
    from models import get_llm_model
    llm = get_llm_model()
    response = llm.invoke("你好")
"""
from typing import Any, Dict, Optional

from loguru import logger
from langchain_openai import ChatOpenAI

from config.settings import settings
from services.redis_service import get_redis_service


class LLMConfigException(Exception):
    """LLM配置加载异常"""


__llm_instance: Optional[ChatOpenAI] = None


def _load_llm_config() -> Dict[str, Any]:
    """
    从Redis读取Java端预热的AI配置（唯一数据源）

    Java端存储字段为驼峰（modelName/apiUrl/apiKey/temperature），
    此处统一转换为Python风格键名（model/api_key/base_url/temperature）

    Raises:
        LLMConfigException: Redis中无AI配置或配置不完整
    """
    redis_service = get_redis_service()

    config = redis_service.get_ai_config()
    if not config:
        logger.error("Redis中无AI配置，请确保Java端已启动并预热AiConfigCacheRunner")
        raise LLMConfigException("AI模型配置未就绪，请稍后重试")

    # 字段映射：Java驼峰 → Python下划线风格
    result = {
        "model": config.get("modelName") or config.get("model_name"),
        "base_url": config.get("apiUrl") or config.get("api_url"),
        "api_key": config.get("apiKey") or config.get("api_key"),
    }

    # 校验必填字段
    missing = [k for k, v in result.items() if not v]
    if missing:
        logger.error("Redis AI配置缺失字段: {}", missing)
        raise LLMConfigException("AI模型配置不完整，请检查管理后台配置")

    # temperature可能是字符串/数字（Java BigDecimal序列化），统一转float
    temp = config.get("temperature")
    if temp is not None:
        try:
            result["temperature"] = float(temp)
        except (TypeError, ValueError):
            pass

    logger.info("LLM配置从Redis加载成功: model={}, base_url={}",
                result.get("model"), result.get("base_url"))
    return result


def get_llm_model() -> ChatOpenAI:
    """
    获取ChatOpenAI聊天模型单例

    配置从Redis读取（Java端预热），包含：
    - model: 模型名称（如 qwen-plus）
    - api_key: API密钥（已解密）
    - base_url: OpenAI兼容接口地址
    - temperature: 温度参数（默认0.1）

    Returns:
        ChatOpenAI 实例

    Raises:
        LLMConfigException: Redis配置未就绪或不完整
    """
    global __llm_instance
    if __llm_instance is None:
        ai_config = _load_llm_config()

        __llm_instance = ChatOpenAI(
            model=ai_config["model"],
            api_key=ai_config["api_key"],
            base_url=ai_config["base_url"],
            temperature=ai_config.get("temperature", 0.1),
            timeout=settings.llm_timeout,
            max_retries=1
        )
        logger.info("LLM聊天模型初始化完成: model={}", ai_config["model"])

    return __llm_instance


def reload_llm_model() -> ChatOpenAI:
    """
    强制重新加载LLM模型（配置变更后调用）

    场景：Java端在管理后台修改了AI配置并刷新Redis后，
    调用此方法使新配置立即生效。

    Returns:
        新的ChatOpenAI实例
    """
    global __llm_instance
    __llm_instance = None
    return get_llm_model()
