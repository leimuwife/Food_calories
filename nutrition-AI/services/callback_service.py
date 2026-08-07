"""Java回调服务 - 通知Java后端入库结果"""
import httpx
import json
from loguru import logger
from typing import Optional
from config.settings import settings
from utils.auth import create_auth_header


class CallbackService:
    """Java回调服务"""

    def __init__(self) -> None:
        self.callback_url = settings.java_callback_url
        self.timeout = 30.0

    async def notify_upload_success(self, doc_id: str, collection_id: str, chunk_count: int) -> bool:
        """
        通知Java后端入库成功

        Args:
            doc_id: 文档ID
            collection_id: 集合ID
            chunk_count: 切片数量

        Returns:
            是否回调成功
        """
        payload = {
            "doc_id": doc_id,
            "collection_id": collection_id,
            "chunk_count": chunk_count,
            "status": "success",
            "message": "文档入库成功"
        }

        return await self._send_callback(payload)

    async def notify_upload_failure(self, doc_id: str, error_message: str) -> bool:
        """
        通知Java后端入库失败

        Args:
            doc_id: 文档ID
            error_message: 错误信息

        Returns:
            是否回调成功
        """
        payload = {
            "doc_id": doc_id,
            "status": "failed",
            "message": error_message
        }

        return await self._send_callback(payload)

    async def notify_delete_success(self, doc_id: str, deleted_count: int) -> bool:
        """
        通知Java后端删除成功

        Args:
            doc_id: 文档ID
            deleted_count: 删除数量

        Returns:
            是否回调成功
        """
        payload = {
            "doc_id": doc_id,
            "deleted_count": deleted_count,
            "status": "success",
            "message": "文档删除成功"
        }

        return await self._send_callback(payload)

    async def notify_update_success(self, doc_id: str, chunk_count: int) -> bool:
        """
        通知Java后端更新成功

        Args:
            doc_id: 文档ID
            chunk_count: 新切片数量

        Returns:
            是否回调成功
        """
        payload = {
            "doc_id": doc_id,
            "chunk_count": chunk_count,
            "status": "success",
            "message": "文档更新成功"
        }

        return await self._send_callback(payload)

    async def _send_callback(self, payload: dict) -> bool:
        """
        发送回调请求

        Args:
            payload: 回调数据

        Returns:
            是否成功
        """
        headers = {
            "Content-Type": "application/json",
            **create_auth_header()
        }

        logger.info("发送回调请求: url={}, payload={}", self.callback_url, payload)

        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    self.callback_url,
                    headers=headers,
                    json=payload
                )

                if response.status_code == 200:
                    result = response.json()
                    logger.info("回调成功: response={}", result)
                    return True
                else:
                    logger.error("回调失败: status_code={}, response={}",
                                 response.status_code, response.text)
                    return False

        except httpx.TimeoutException:
            logger.error("回调超时: url={}", self.callback_url)
            return False
        except httpx.ConnectError as e:
            logger.error("回调连接失败: url={}, error={}", self.callback_url, str(e))
            return False
        except Exception as e:
            logger.error("回调异常: url={}, error={}", self.callback_url, str(e))
            return False
