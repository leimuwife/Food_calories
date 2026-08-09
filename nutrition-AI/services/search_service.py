"""向量检索服务 - 知识库问答检索

设计说明：
- 与入库服务（VectorService）解耦，专注于检索业务逻辑
- 遵循项目硬约束"向量库实例必须全局单例复用连接"，
  通过复用 VectorService 单例的 collection 和 embeddings 避免重复初始化连接
- 入库时为文本添加 "document: " 前缀，检索返回时去除该前缀
"""
import time
from loguru import logger
from typing import List, Optional

from langchain_community.embeddings.dashscope import DashScopeEmbeddings
# 改动3: 引入tenacity重试组件，仅对向量库网络/超时异常轻量重试
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception

from constants.global_constants import VectorConstants
from services.vector_service import get_vector_service


class SearchService:
    """知识库向量检索服务"""

    def __init__(self) -> None:
        # 复用 VectorService 单例的 collection 和 embeddings
        # （项目硬约束：向量库实例全局单例，复用连接，禁止重复初始化）
        vec_service = get_vector_service()
        self.collection = vec_service.collection
        self.embeddings: DashScopeEmbeddings = vec_service.embeddings
        logger.info("SearchService 初始化完成（复用 VectorService 单例连接）")

    # ==================== 参数校验 ====================

    @staticmethod
    def _validate_query(query: str) -> bool:
        """校验query是否有效"""
        return query is not None and isinstance(query, str) and len(query.strip()) > 0

    @staticmethod
    def _validate_topk(topk: int) -> bool:
        # 改动1: 对外入参topk固定约束为8~12
        return (topk is not None and isinstance(topk, int)
                and VectorConstants.RETRIEVE_MIN_TOPK <= topk <= VectorConstants.RETRIEVE_MAX_TOPK)

    # ==================== 改动3: 网络/超时异常重试 ====================

    @staticmethod
    def _is_network_or_timeout(exc: Exception) -> bool:
        """
        判断异常是否为网络/超时类（仅此类异常触发重试）
        业务报错、参数错误不在此列，不会触发重试。
        覆盖 gRPC 超时（如 DashVector "deadline exceeded"）及通用网络异常。
        """
        if isinstance(exc, (ConnectionError, TimeoutError, OSError)):
            return True
        msg = str(exc).lower()
        return any(kw in msg for kw in (
            "timeout", "deadline exceeded", "unavailable",
            "connection reset", "transport", "rpc error"
        ))

    @retry(
        retry=retry_if_exception(_is_network_or_timeout),
        # 最大重试次数2次（初始1次 + 重试2次 = 共3次尝试）
        stop=stop_after_attempt(3),
        # 指数退避：初始0.1s，最长等待0.5s
        wait=wait_exponential(multiplier=0.1, max=0.5),
        reraise=True
    )
    def _execute_query(self, query_vector, topk: int):
        """
        执行向量库检索（改动3: 仅对网络/超时异常轻量重试，业务错误不重试）
        业务层返回的带错误码响应对象不抛异常，因此不会触发重试。
        """
        return self.collection.query(
            vector=query_vector,
            topk=topk,
            output_fields=["text", "doc_id", "file_md5", "chunk_index", "filename"],
            include_vector=False
        )

    # ==================== 核心业务方法 ====================

    def search(self, query: str, topk: int = 5) -> List[dict]:
        """
        知识库向量检索

        改动1-召回-重排-截取规则：
        1. topk入参固定约束8~12
        2. 按传入topk召回原始结果
        3. 相似度阈值过滤后基于score降序重排
        4. 最终返回 topk-3 条；不足则返回全部合格结果；空结果直接返回空列表
        """
        logger.info("向量检索: query={}, topk={}", query[:50], topk)

        if not self._validate_query(query):
            logger.warning("query无效，跳过检索")
            return []

        if not self._validate_topk(topk):
            logger.warning("topk无效({})，限制为{}-{}",
                           topk, VectorConstants.RETRIEVE_MIN_TOPK, VectorConstants.RETRIEVE_MAX_TOPK)
            topk = max(VectorConstants.RETRIEVE_MIN_TOPK, min(topk, VectorConstants.RETRIEVE_MAX_TOPK))

        try:
            # 生成查询向量（耗时埋点）
            embed_start = time.time()
            query_vector = self.embeddings.embed_query(query)
            embed_cost = (time.time() - embed_start) * 1000
            logger.info("检索嵌入耗时: query={}, cost={:.1f}ms", query[:50], embed_cost)

            # 执行检索（耗时埋点 + 改动3: 网络/超时异常自动重试）
            # 改动1: 按传入topk召回原始结果
            search_start = time.time()
            results = self._execute_query(query_vector, topk)
            search_cost = (time.time() - search_start) * 1000
            logger.info("检索耗时: query={}, cost={:.1f}ms, raw_count={}", query[:50], search_cost, len(results))

            # 整理结果：
            # 【注意】此处不做硬性距离阈值过滤，将裁决权交给上层业务：
            #   - 食物名称查询场景：query和匹配名都很短，DashVector余弦对括号后缀不敏感，
            #     "鸡蛋（红皮）"等正确结果的distance可能落在0.25~0.35之间，
            #     过滤掉反而丢失正确条目。业务层可结合字面量匹配加分重排。
            #   - 普通知识库文档检索：上层可自行设置合理阈值。
            # 仅记录统计日志，便于观察"距离偏大"的条目数量。
            search_results = []
            soft_filtered_count = 0

            for doc in results:
                score = float(doc.score) if hasattr(doc, "score") else 0.0
                raw_text = doc.fields.get("text", "")
                filename = doc.fields.get("filename", "")
                if score > VectorConstants.SEARCH_SCORE_THRESHOLD:
                    soft_filtered_count += 1
                    logger.debug("  距离偏大(未强过滤): score={:.4f}, filename={}, text={}",
                                 score, filename, raw_text[:60])
                else:
                    logger.info("  召回结果: score={:.4f}, filename={}, text={}",
                                score, filename, raw_text[:80])

                # 去除入库时添加的 document: 前缀，返回纯净文本
                clean_text = raw_text[len(VectorConstants.DOCUMENT_PREFIX):] if raw_text.startswith(VectorConstants.DOCUMENT_PREFIX) else raw_text

                search_results.append({
                    "text": clean_text,
                    "score": score,
                    "doc_id": doc.fields.get("doc_id", ""),
                    "file_md5": doc.fields.get("file_md5", ""),
                    "chunk_index": int(doc.fields.get("chunk_index", 0)),
                    "filename": doc.fields.get("filename", "")
                })

            if soft_filtered_count > 0:
                logger.info("检索距离分布: 原始={}, distance>{}(参考阈值)={}, 全部保留供业务裁决={}",
                            len(results), VectorConstants.SEARCH_SCORE_THRESHOLD, soft_filtered_count, len(search_results))

            # DashVector cosine distance: 升序排列（距离越小越相似，排在前面）
            search_results.sort(key=lambda x: x["score"])

            # 改动1: 截取 topk-3 条（不足则返回全部合格结果；空列表直接返回，不做任何兜底填充）
            final_count = topk - 3
            final_results = search_results[:final_count]

            total_cost = embed_cost + search_cost
            logger.info("向量检索完成: query={}, recall={}, soft_over_threshold={}, rerank_returned={}, topk-3={}, total_cost={:.1f}ms",
                        query[:50], len(results), soft_filtered_count, len(final_results), final_count, total_cost)
            return final_results

        except Exception as e:
            logger.error("向量检索失败: query={}, error={}", query[:50], str(e))
            raise


# -------------------------- 懒加载单例入口 --------------------------
__search_instance: Optional["SearchService"] = None


def get_search_service() -> SearchService:
    """获取SearchService单例实例（首次调用初始化，后续复用）"""
    global __search_instance
    if __search_instance is None:
        __search_instance = SearchService()
    return __search_instance
