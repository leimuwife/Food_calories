"""AI聊天会话服务 - 会话缓存与持久化封装

整体职责：
    封装全部会话的存取逻辑，上层 ReAct-Agent 只调用本模块的对外公有方法，
    无需关心 Redis 缓存、Java-MySQL 回调的底层细节。

设计说明：
1. MySQL 所有 CRUD 通过 HTTP 回调 Java 后端接口（/api/chat/session/*）完成，
   Python 不直接连接数据库、不编写任何 SQL。
2. Redis 由 Python 直接读写，key 格式：active:session:{session_id}
   Value 为 Redis List，每个元素是一条 JSON 结构化消息 {"role":..., "content":...}，
   缓存过期时间 12 小时。
3. 消息先写 Redis 缓存（append_message 即时写，延迟落盘），
   在缓存过期/会话关闭/新会话开启/手动清理等时机统一批量落盘 MySQL，
   保证缓存淘汰不影响 MySQL 持久化数据。
4. 提供后台 TTL 扫描线程（start_session_sweeper），缓存临近过期自动提前落盘，
   防止 12 小时缓存过期导致未持久化消息丢失。

对外公有方法：
    create_session(user_id)              创建全新聊天会话，返回 session_id
    get_recent_history(session_id)       获取最近多轮对话上下文（供LLM）
    append_message(session_id, role, content)  追加一条会话消息
    clear_session_cache(session_id)      仅清除Redis热点缓存（先落盘再删除）
    flush_session_to_mysql(session_id)   批量落盘工具函数
"""
import json
import threading
import time
from typing import Any, Dict, List, Optional

import httpx
import redis
from loguru import logger

from config.settings import settings
from constants.global_constants import RedisConstants, ChatConstants
from services.redis_service import get_redis_service
from utils.auth import create_auth_header


class SessionServiceException(Exception):
    """会话服务异常（Java接口调用失败 / 参数非法 / Redis读写异常）"""


class SessionService:
    """AI聊天会话服务（懒加载单例）"""

    def __init__(self) -> None:
        # 复用全局Redis单例连接（遵循项目全局单例复用连接约束）
        self.redis = get_redis_service()
        self.java_base_url = settings.java_base_url.rstrip("/")
        # RestTemplate/httpx 超时：连接10s，读写30s（Java落盘批量插入耗时）
        self.http_timeout = httpx.Timeout(30.0, connect=10.0)

    # ==================== 对外公有方法 ====================

    def create_session(self, user_id) -> str:
        """
        创建全新聊天会话

        执行流程：
        1. 回调Java接口，向chat_session表插入一条会话记录（session_id由Java雪花生成）
        2. 在Redis初始化该session_id对应的空消息列表缓存
        3. 返回session_id（字符串）给调用方

        Args:
            user_id: 用户ID（必传，类比豆包当前用户维度）

        Returns:
            新会话ID（字符串）

        Raises:
            SessionServiceException: Java创建失败或user_id为空
        """
        if user_id is None or str(user_id).strip() == "":
            raise SessionServiceException("user_id不能为空")

        logger.info("创建AI聊天会话: user_id={}", user_id)
        payload = {"userId": int(user_id)}

        data = self._post_json("/chat/session/create", payload, "创建会话")
        session_id = (data or {}).get("sessionId")
        if not session_id:
            raise SessionServiceException("Java创建会话未返回sessionId")

        session_id = str(session_id)

        # 初始化Redis空消息列表缓存（12h过期）
        self._reset_cache(session_id)

        logger.info("会话创建成功: session_id={}, user_id={}", session_id, user_id)
        return session_id

    def get_recent_history(self, session_id: str, max_turns: int = 10) -> List[Dict[str, str]]:
        """
        获取该会话最近8-10轮结构化对话上下文，供给LLM使用

        执行流程：
        1. 优先读Redis缓存 active:session:{session_id}
        2. 缓存命中且非空：直接返回消息数组
        3. 缓存未命中/已过期：回调Java查询未被逻辑删除的最近消息并写入缓存

        上下文裁剪规则：仅加载最近多轮交互给到大模型，
        更早历史永久保存在MySQL，不作加载。

        Args:
            session_id: 会话ID
            max_turns: 最多返回轮数（每轮约2条消息），默认10

        Returns:
            结构化消息列表 [{"role": ..., "content": ...}, ...]；
            Redis/Java异常时返回空列表（不阻断上层Agent链路）

        Raises:
            SessionServiceException: session_id为空
        """
        if not session_id:
            raise SessionServiceException("session_id不能为空")

        # 1. 优先读缓存
        cached = self._read_cache(session_id)
        if cached:
            limit = max(1, max_turns) * 2
            recent = cached[-limit:]
            logger.info("会话历史-缓存命中: session_id={}, cached={}, return={}",
                        session_id, len(cached), len(recent))
            return recent

        # 2. 缓存未命中：回调Java查询最近消息
        logger.info("会话历史-缓存未命中，回调Java查询: session_id={}", session_id)
        limit = max(1, max_turns) * 2
        try:
            messages = self._fetch_history_from_java(session_id, limit)
        except SessionServiceException as e:
            logger.error("会话历史-回调Java失败: session_id={}, error={}", session_id, str(e))
            return []

        # 3. 写回Redis缓存（12h有效）
        self._overwrite_cache(session_id, messages)

        logger.info("会话历史-加载完成: session_id={}, count={}", session_id, len(messages))
        return messages

    def append_message(self, session_id: str, role: str, content: str) -> None:
        """
        追加一条会话消息（仅更新Redis缓存，延迟落盘MySQL）

        role 合法取值：user / ai_thought / tool_call / tool_result / ai_answer

        执行流程：
        1. 将结构化消息追加到Redis对应消息列表
        2. 更新缓存过期时间为12小时
        （即时MySQL写入由 flush_session_to_mysql 在落盘时机统一完成）

        Args:
            session_id: 会话ID
            role: 消息角色
            content: 消息内容

        Raises:
            SessionServiceException: 参数非法或Redis写入异常
        """
        if not session_id:
            raise SessionServiceException("session_id不能为空")
        if role not in ChatConstants.VALID_ROLES:
            raise SessionServiceException(f"非法消息角色: {role}，可选值: {ChatConstants.VALID_ROLES}")
        if content is None or str(content).strip() == "":
            raise SessionServiceException("消息内容不能为空")

        msg = {"role": role, "content": content}
        key = self._cache_key(session_id)
        try:
            self.redis.client.rpush(key, json.dumps(msg, ensure_ascii=False).encode("utf-8"))
            self.redis.client.expire(key, RedisConstants.SESSION_CACHE_TTL_SECONDS)
        except redis.RedisError as e:
            logger.warning("会话消息-缓存追加异常: session_id={}, error={}", session_id, str(e))
            raise SessionServiceException("Redis缓存写入失败") from e

        logger.info("会话消息-已追加缓存: session_id={}, role={}, cache_size_after={}",
                    session_id, role, len(self._read_cache(session_id)))

    def clear_session_cache(self, session_id: str) -> None:
        """
        仅手动清除Redis热点缓存（MySQL数据完整保留）

        执行流程：
        1. 前置操作：先把Redis中未持久化的消息批量落盘MySQL
        2. 删除Redis缓存 active:session:{session_id}

        Args:
            session_id: 会话ID

        Raises:
            SessionServiceException: session_id为空或落盘失败（此时缓存保留可重试）
        """
        if not session_id:
            raise SessionServiceException("session_id不能为空")

        # 1. 先落盘未持久化消息
        self.flush_session_to_mysql(session_id)

        # 2. 删除Redis缓存（幂等，key不存在也不报错）
        self._delete_cache(session_id)
        logger.info("会话缓存-已清除: session_id={}", session_id)

    def flush_session_to_mysql(self, session_id: str) -> None:
        """
        批量落盘工具函数：将Redis内未持久化的消息批量写入MySQL

        执行流程：
        1. 获取Redis内完整消息数组
        2. 消息列表为空直接结束
        3. 回调Java接口：会话不存在则插入chat_session；批量提交全部消息
        4. 落盘成功后清空该session对应的Redis缓存

        Args:
            session_id: 会话ID

        Raises:
            SessionServiceException: session_id为空或Java落盘失败
                （失败时缓存保留，调用方可重试）
        """
        if not session_id:
            raise SessionServiceException("session_id不能为空")

        messages = self._read_cache(session_id)
        if not messages:
            logger.info("会话落盘-无待落盘消息，跳过: session_id={}", session_id)
            return

        logger.info("会话落盘-开始批量落盘: session_id={}, count={}", session_id, len(messages))
        payload = {
            "sessionId": int(session_id),
            "messages": messages,
        }
        self._post_json("/chat/session/flush", payload, "批量落盘")

        # 落盘成功后清空缓存（注意：清空而非删除整个key的过期重置）
        self._reset_cache(session_id)
        logger.info("会话落盘-完成: session_id={}, count={}", session_id, len(messages))

    # ==================== 内部：Java HTTP调用 ====================

    def _post_json(self, path: str, payload: Dict[str, Any], action: str) -> Optional[Dict[str, Any]]:
        """
        发送POST请求到Java后端，校验统一响应结构 {code, message, data}

        Args:
            path: 接口路径（如 /chat/session/create）
            payload: 请求体
            action: 操作名称（用于日志）

        Returns:
            响应data字段（字典）

        Raises:
            SessionServiceException: HTTP/连接/响应解析异常或业务失败
        """
        url = f"{self.java_base_url}{path}"
        headers = {"Content-Type": "application/json", **create_auth_header()}
        logger.info("会话-回调Java[{}]: url={}, payload={}", action, url, payload)

        try:
            with httpx.Client(timeout=self.http_timeout) as client:
                resp = client.post(url, headers=headers, json=payload)

            if resp.status_code != 200:
                logger.error("会话-回调Java[{}]失败: status_code={}, body={}",
                             action, resp.status_code, resp.text[:500])
                raise SessionServiceException(f"Java接口[{action}]返回异常状态码: {resp.status_code}")

            body = resp.json()
            code = body.get("code")
            # 兼容Java端返回字符串"200"或整数200
            if str(code) != "200":
                logger.error("会话-回调Java[{}]业务失败: code={}, message={}",
                             action, code, body.get("message"))
                raise SessionServiceException(body.get("message") or f"Java接口[{action}]业务失败")

            return body.get("data")
        except httpx.TimeoutException:
            logger.error("会话-回调Java[{}]超时: url={}", action, url)
            raise SessionServiceException(f"Java接口[{action}]调用超时") from None
        except httpx.ConnectError:
            logger.error("会话-回调Java[{}]连接失败: url={}", action, url)
            raise SessionServiceException(f"Java接口[{action}]连接失败，请确认Java后端已启动") from None
        except SessionServiceException:
            raise
        except Exception as e:
            logger.error("会话-回调Java[{}]异常: url={}, error={}", action, url, str(e))
            raise SessionServiceException(f"Java接口[{action}]调用异常") from e

    def _fetch_history_from_java(self, session_id: str, limit: int) -> List[Dict[str, str]]:
        """
        回调Java查询最近历史消息（未被逻辑删除，正序返回）

        Args:
            session_id: 会话ID
            limit: 最多返回条数

        Returns:
            消息列表 [{"role":..., "content":...}, ...]；Java返回空则空列表
        """
        path = f"/chat/session/{session_id}/history?limit={limit}"
        url = f"{self.java_base_url}{path}"
        headers = create_auth_header()
        logger.info("会话-回调Java[查询历史]: url={}", url)

        try:
            with httpx.Client(timeout=self.http_timeout) as client:
                resp = client.get(url, headers=headers)

            if resp.status_code != 200:
                logger.error("会话-回调Java[查询历史]失败: status_code={}, body={}",
                             resp.status_code, resp.text[:500])
                raise SessionServiceException(f"Java接口[查询历史]返回异常状态码: {resp.status_code}")

            body = resp.json()
            code = body.get("code")
            # 兼容Java端返回字符串"200"或整数200
            if str(code) != "200":
                logger.error("会话-回调Java[查询历史]业务失败: code={}, message={}",
                             code, body.get("message"))
                raise SessionServiceException(body.get("message") or "Java接口[查询历史]业务失败")

            data = body.get("data") or []
            messages = []
            for item in data:
                role = item.get("role")
                content = item.get("content")
                if role and content is not None:
                    messages.append({"role": role, "content": content})
            return messages
        except httpx.TimeoutException:
            logger.error("会话-回调Java[查询历史]超时: url={}", url)
            raise SessionServiceException("Java接口[查询历史]调用超时") from None
        except httpx.ConnectError:
            logger.error("会话-回调Java[查询历史]连接失败: url={}", url)
            raise SessionServiceException("Java接口[查询历史]连接失败，请确认Java后端已启动") from None
        except SessionServiceException:
            raise
        except Exception as e:
            logger.error("会话-回调Java[查询历史]异常: url={}, error={}", url, str(e))
            raise SessionServiceException("Java接口[查询历史]调用异常") from e

    # ==================== 内部：Redis缓存操作 ====================

    @staticmethod
    def _cache_key(session_id: str) -> str:
        """构造Redis缓存key: active:session:{session_id}"""
        return f"{RedisConstants.SESSION_CACHE_PREFIX}{session_id}"

    def _read_cache(self, session_id: str) -> List[Dict[str, str]]:
        """
        读取Redis中该会话的完整消息数组

        Returns:
            消息列表；缓存不存在/为空/解析异常均返回空列表
        """
        key = self._cache_key(session_id)
        try:
            raw_list = self.redis.client.lrange(key, 0, -1)
            messages = []
            for raw in raw_list:
                text = raw.decode("utf-8") if isinstance(raw, bytes) else str(raw)
                try:
                    messages.append(json.loads(text))
                except json.JSONDecodeError:
                    logger.warning("会话缓存-消息解析失败，跳过: session_id={}, raw={}",
                                   session_id, text[:200])
            return messages
        except redis.RedisError as e:
            logger.warning("会话缓存-读取异常: session_id={}, error={}", session_id, str(e))
            return []
        except Exception as e:
            logger.warning("会话缓存-读取未知异常: session_id={}, error={}", session_id, str(e))
            return []

    def _reset_cache(self, session_id: str) -> None:
        """
        重置Redis缓存为空消息列表（同时设置12h过期）
        用于：创建会话时初始化、落盘成功后清空缓存
        """
        key = self._cache_key(session_id)
        try:
            self.redis.client.delete(key)
            logger.debug("会话缓存-已重置为空: session_id={}", session_id)
        except redis.RedisError as e:
            logger.warning("会话缓存-重置异常: session_id={}, error={}", session_id, str(e))
            raise SessionServiceException("Redis缓存重置失败") from e

    def _delete_cache(self, session_id: str) -> None:
        """删除Redis缓存（幂等）"""
        key = self._cache_key(session_id)
        try:
            self.redis.client.delete(key)
        except redis.RedisError as e:
            logger.warning("会话缓存-删除异常: session_id={}, error={}", session_id, str(e))
            raise SessionServiceException("Redis缓存删除失败") from e

    def _overwrite_cache(self, session_id: str, messages: List[Dict[str, str]]) -> None:
        """
        覆写Redis缓存为指定消息数组（12h过期）
        用于：get_recent_history 缓存未命中时写入从Java查询的历史
        """
        key = self._cache_key(session_id)
        try:
            pipe = self.redis.client.pipeline()
            pipe.delete(key)
            for msg in messages:
                pipe.rpush(key, json.dumps(msg, ensure_ascii=False).encode("utf-8"))
            pipe.expire(key, RedisConstants.SESSION_CACHE_TTL_SECONDS)
            pipe.execute()
        except redis.RedisError as e:
            logger.warning("会话缓存-覆写异常: session_id={}, count={}, error={}",
                           session_id, len(messages), str(e))
            raise SessionServiceException("Redis缓存写入失败") from e

    # ==================== 内部：后台TTL扫描 ====================

    def _sweep_expiring_sessions(self) -> int:
        """
        扫描所有 active:session:* 缓存，对剩余TTL低于阈值的会话执行批量落盘

        触发时机：Redis缓存临近12h过期，防止缓存过期导致未持久化消息丢失

        Returns:
            本次落盘的会话数
        """
        flushed = 0
        try:
            keys = self.redis.client.keys(f"{RedisConstants.SESSION_CACHE_PREFIX}*")
            for key in keys:
                session_id = key.decode("utf-8").split(":", 2)[2]
                try:
                    ttl = self.redis.client.ttl(key)
                except redis.RedisError as e:
                    logger.warning("会话TTL扫描-查询TTL异常: session_id={}, error={}", session_id, str(e))
                    continue

                if ttl is None or ttl < 0:
                    # 无TTL或key不存在，跳过
                    continue
                if ttl <= RedisConstants.SESSION_TTL_SWEEP_THRESHOLD_SECONDS:
                    logger.info("会话TTL扫描-缓存临近过期，提前落盘: session_id={}, ttl={}s",
                                session_id, ttl)
                    try:
                        self.flush_session_to_mysql(session_id)
                        flushed += 1
                    except SessionServiceException as e:
                        logger.error("会话TTL扫描-落盘失败: session_id={}, error={}",
                                     session_id, str(e))
        except redis.RedisError as e:
            logger.warning("会话TTL扫描-遍历异常: error={}", str(e))
        except Exception as e:
            logger.warning("会话TTL扫描-未知异常: error={}", str(e))
        return flushed

    def start_sweeper(self) -> None:
        """
        启动后台TTL扫描线程（守护线程，随进程退出）
        扫描间隔：SESSION_SWEEP_INTERVAL_SECONDS（默认5分钟）
        """
        def _loop() -> None:
            logger.info("会话TTL扫描线程已启动: interval={}s, threshold={}s",
                        RedisConstants.SESSION_SWEEP_INTERVAL_SECONDS,
                        RedisConstants.SESSION_TTL_SWEEP_THRESHOLD_SECONDS)
            while True:
                try:
                    time.sleep(RedisConstants.SESSION_SWEEP_INTERVAL_SECONDS)
                    flushed = self._sweep_expiring_sessions()
                    if flushed > 0:
                        logger.info("会话TTL扫描完成: flushed={}", flushed)
                except Exception as e:
                    logger.warning("会话TTL扫描线程异常: error={}", str(e))

        thread = threading.Thread(target=_loop, name="session-ttl-sweeper", daemon=True)
        thread.start()


# -------------------------- 懒加载单例入口 --------------------------
__session_instance: Optional[SessionService] = None


def get_session_service() -> SessionService:
    """获取SessionService单例实例（首次调用初始化，后续复用）"""
    global __session_instance
    if __session_instance is None:
        __session_instance = SessionService()
    return __session_instance


def start_session_sweeper() -> None:
    """启动会话TTL后台扫描（在FastAPI启动时调用）"""
    get_session_service().start_sweeper()
