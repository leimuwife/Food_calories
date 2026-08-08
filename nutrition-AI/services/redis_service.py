"""Redis服务 - 食材基础热量缓存查询

设计说明：
- 与Java后端共用同一个Redis，读取Java预热的食材营养Hash缓存（key=food:nutrition）
- Java端写入时使用 GenericJackson2JsonRedisSerializer 序列化 String，
  Redis中实际存储的可能是带类型信息或双重引号的JSON字符串，需兼容解析
- 遵循项目硬约束"全局单例复用连接"，懒加载初始化并复用连接池
"""
import json
import time
from typing import Any, Dict, Optional

import redis
from loguru import logger

from config.settings import settings
from constants.global_constants import RedisConstants


class RedisService:
    """Redis缓存查询服务（懒加载单例）"""

    def __init__(self) -> None:
        self.client = redis.Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password or None,
            db=settings.redis_db,
            decode_responses=False,          # 保留bytes，便于兼容Java序列化格式解析
            socket_connect_timeout=3,        # 连接超时3秒
            socket_timeout=5,                # 读写超时5秒
            retry_on_timeout=True,
            protocol=2                        # 强制RESP2协议，避免低版本Redis不支持HELLO命令
        )
        logger.info("Redis服务初始化完成: host={}, port={}, db={}",
                    settings.redis_host, settings.redis_port, settings.redis_db)

    # ==================== 辅助方法 ====================

    @staticmethod
    def _parse_value(raw: Any) -> Optional[Dict[str, Any]]:
        """
        兼容解析Redis中由Java GenericJackson2JsonRedisSerializer序列化的值

        Java写入的String可能呈现为：
        1. 普通JSON字符串: {"calorie":53.0,"protein":0.4,...}
        2. 双重JSON字符串: "{\"calorie\":53.0,...}"（JSON字符串再被转义）
        3. 数组类型包装: ["java.lang.String","{\"calorie\":53.0,...}"]
        4. 对象类型包装: {"@class":"java.lang.String","value":"{...}"}

        Args:
            raw: Redis原始值（bytes或str）

        Returns:
            解析后的营养字典；无法解析返回None
        """
        if raw is None:
            return None

        # bytes -> str
        text = raw.decode("utf-8") if isinstance(raw, bytes) else str(raw)

        for _ in range(5):  # 最多解析5层引号/类型包装，防止深度嵌套
            text = text.strip()
            if not text:
                return None

            # 先尝试整体JSON解析；解析结果为字符串（双重JSON字符串）则继续解析该字符串
            try:
                obj = json.loads(text)
            except json.JSONDecodeError:
                # 非JSON内容（如普通文本），剥掉外层引号后继续
                if len(text) >= 2 and text[0] in ('"', "'") and text[-1] == text[0]:
                    text = text[1:-1]
                    continue
                logger.debug("Redis值无法解析为JSON: {}", text[:200])
                return None

            # 双重JSON字符串: "{\"calorie\":53.0,...}" → 得到内层字符串继续解析
            if isinstance(obj, str):
                text = obj
                continue

            # 兼容Java数组类型包装: ["java.lang.String","value"] → 取第二个元素继续解析
            if isinstance(obj, list) and len(obj) == 2 and isinstance(obj[1], str):
                text = obj[1]
                continue

            # 兼容Java对象类型包装: {"@class":"java.lang.String","value":"..."} → 取value继续解析
            if isinstance(obj, dict) and "@class" in obj and "value" in obj:
                text = obj.get("value", "")
                continue

            # 最终结果必须是营养字典
            return obj if isinstance(obj, dict) else None

        return None

    # ==================== 核心业务方法 ====================

    def get_food_nutrition(self, food_name: str) -> Optional[Dict[str, Any]]:
        """
        按食物名称查询Hash缓存中的基础营养数据（每100g含量）

        Args:
            food_name: 食物名称

        Returns:
            营养字典（含 calorie/protein/fat/carbohydrate，均为每100g值）；
            未命中返回None
        """
        try:
            start = time.time()
            raw = self.client.hget(RedisConstants.FOOD_NUTRITION_HASH_KEY, food_name)
            cost = (time.time() - start) * 1000

            if raw is None:
                logger.info("Redis缓存未命中: food_name={}, hash_key={}, cost={:.1f}ms",
                            food_name, RedisConstants.FOOD_NUTRITION_HASH_KEY, cost)
                return None

            data = self._parse_value(raw)
            if data is None:
                logger.warning("Redis缓存值解析失败: food_name={}, raw={}", food_name, str(raw)[:200])
                return None

            logger.info("Redis缓存命中: food_name={}, data={}, cost={:.1f}ms",
                        food_name, data, cost)
            return data

        except redis.RedisError as e:
            logger.warning("Redis查询异常（记录日志，不阻断链路）: food_name={}, error={}",
                           food_name, str(e))
            return None
        except Exception as e:
            logger.warning("Redis查询未知异常（记录日志，不阻断链路）: food_name={}, error={}",
                           food_name, str(e))
            return None

    def get_ai_config(self) -> Optional[Dict[str, Any]]:
        """
        读取当前启用的AI模型配置（由Java端 AiConfigCacheRunner 预热写入）

        Redis结构：String，key=ai:config:enabled
        包含字段：modelName, apiUrl, apiKey(已解密), temperature, maxTokens, systemPrompt

        Returns:
            AI配置字典；未命中返回None
        """
        try:
            start = time.time()
            raw = self.client.get(RedisConstants.AI_CONFIG_ENABLED_KEY)
            cost = (time.time() - start) * 1000

            if raw is None:
                logger.info("Redis AI配置未命中: key={}, cost={:.1f}ms",
                            RedisConstants.AI_CONFIG_ENABLED_KEY, cost)
                return None

            data = self._parse_value(raw)
            if data is None:
                logger.warning("Redis AI配置解析失败: raw={}", str(raw)[:200])
                return None

            logger.info("Redis AI配置命中: model={}, cost={:.1f}ms",
                        data.get("modelName"), cost)
            return data

        except redis.RedisError as e:
            logger.warning("Redis AI配置查询异常（记录日志，不阻断链路）: error={}", str(e))
            return None
        except Exception as e:
            logger.warning("Redis AI配置查询未知异常（记录日志，不阻断链路）: error={}", str(e))
            return None


# -------------------------- 懒加载单例入口 --------------------------
__redis_instance: Optional[RedisService] = None


def get_redis_service() -> RedisService:
    """获取RedisService单例实例（首次调用初始化，后续复用）"""
    global __redis_instance
    if __redis_instance is None:
        __redis_instance = RedisService()
    return __redis_instance
