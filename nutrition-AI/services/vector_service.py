"""向量库服务 - DashVector封装"""
import time
import dashvector
from dashvector import Client, Doc
from loguru import logger
from typing import List, Optional, Dict, Any
from langchain_core.documents import Document
from langchain_community.embeddings.dashscope import DashScopeEmbeddings
from config.settings import settings
from constants.global_constants import VectorConstants


class VectorService:
    def __init__(self) -> None:
        api_key = settings.vector_api_key.strip()
        endpoint = settings.vector_endpoint.strip()
        logger.info("正在初始化DashVector客户端")

        self.client = dashvector.Client(
            api_key=api_key,
            endpoint=endpoint
        )

        # 鉴权校验：注意 DashVector SDK 的 client.get() 在鉴权失败时不抛异常，
        # 而是返回带错误码的对象（如 code=-2980 "Token dont exist"），
        # 必须显式检查返回对象的 code 字段，否则会出现"假通过"误判
        test_conn = self.client.get(name=settings.vector_collection_name)
        _code = getattr(test_conn, "code", 0)
        if _code not in (0, None, ""):
            _msg = getattr(test_conn, "message", "N/A")
            logger.error("DashVector鉴权失败: code={}, message={}", _code, _msg)
            raise RuntimeError(
                f"向量数据库密钥或Endpoint配置错误，请检查 VECTOR_API_KEY / VECTOR_ENDPOINT "
                f"(服务端返回: code={_code}, message={_msg})"
            )
        logger.info("DashVector 客户端鉴权连通检测通过")

        self.embeddings = DashScopeEmbeddings(
            model=settings.embedding_model,
            dashscope_api_key=settings.dashscope_api_key
        )

        self.collection = self._get_or_create_collection()

        logger.info("DashVector服务初始化完成, collection={}, dimension={}",
                    settings.vector_collection_name, VectorConstants.VECTOR_DIMENSION)

    def _get_or_create_collection(self):
        collection = self.client.get(name=settings.vector_collection_name)
        code = getattr(collection, "code", 0)
        msg = str(getattr(collection, "message", ""))

        if code in (0, None, ""):
            logger.info("获取已存在的集合: {}", settings.vector_collection_name)
            return collection

        # 鉴权类错误直接抛出（如 -2980 Token dont exist / 401 InvalidApiKey）
        if code in (-2980, 401, 403) or any(
                kw in msg.lower() for kw in ("token", "api-key", "apikey", "auth")):
            raise RuntimeError(f"DashVector鉴权失败，请检查密钥/Endpoint: code={code}, message={msg}")

        # 集合不存在时创建
        logger.info("集合不存在，创建新集合: {}, dimension={}",
                    settings.vector_collection_name, VectorConstants.VECTOR_DIMENSION)
        self.client.create(
            name=settings.vector_collection_name,
            dimension=VectorConstants.VECTOR_DIMENSION,
            metric="cosine"
        )
        created = self.client.get(name=settings.vector_collection_name)
        if getattr(created, "code", 0) not in (0, None, ""):
            raise RuntimeError(f"创建集合失败: code={getattr(created, 'code', 'N/A')}, "
                               f"message={getattr(created, 'message', 'N/A')}")
        return created

    # ==================== 辅助方法 ====================

    @staticmethod
    def _validate_doc_id(doc_id: str) -> bool:
        """校验doc_id是否有效"""
        return doc_id is not None and isinstance(doc_id, str) and len(doc_id.strip()) > 0

    @staticmethod
    def _build_filter_expr(field: str, value: str) -> str:
        """
        安全构造DashVector过滤表达式
        转义特殊字符，防止注入
        DashVector filter格式: field = 'value'
        """
        safe_value = value.replace("\\", "\\\\").replace("'", "\\'")
        return f"{field} = '{safe_value}'"

    def _query_with_filter(self, filter_params: Dict[str, Any], topk: int = 1) -> List:
        """
        使用过滤条件查询向量库

        注意：DashVector SDK 1.0.x 的 query() 参数 filter 仅支持字符串表达式
        （如 doc_id = '123'），不支持 dict 格式 —— 传 dict 时 SDK 不抛异常，
        而是返回 code=-2999 的错误响应对象，所以必须直接构造字符串表达式，
        并显式检查返回对象的 code 字段。

        Args:
            filter_params: 过滤条件字典，如 {"doc_id": "123"}
            topk: 返回结果数量

        Returns:
            查询结果列表（每项含 id / fields / score）

        Raises:
            RuntimeError: 查询失败（code 非 0）时抛出
        """
        # 将过滤条件字典构造成字符串表达式: field = 'value' AND ...
        filter_exprs = [
            self._build_filter_expr(field, str(value))
            for field, value in filter_params.items()
        ]
        filter_expr = " AND ".join(filter_exprs)

        results = self.collection.query(
            vector=self.embeddings.embed_query("dummy"),
            topk=topk,
            filter=filter_expr,
            output_fields=list(filter_params.keys()),
            include_vector=False
        )

        # SDK 查询失败时不抛异常，而是返回带错误码的对象，必须显式检查
        code = getattr(results, "code", 0)
        if code not in (0, None, ""):
            raise RuntimeError(
                f"向量库查询失败: code={code}, "
                f"message={getattr(results, 'message', 'N/A')}"
            )

        # 成功时返回 output 列表（每项含 id/fields/score），供调用方 len()/遍历
        return results.output if hasattr(results, "output") else results

    # ==================== 核心业务方法 ====================

    def insert_documents(self, documents: List[Document], doc_id: str, file_md5: str = "") -> int:
        """
        将文档切片分批插入向量库
        使用BATCH_SIZE=50分批循环insert，避免单次请求过大

        基于DashVector官方SDK insert接口：
        - collection.insert(docs: List[Doc]) → DashVectorResponse
        - 返回值包含 code(0=成功), message, output, usage(写请求单元数)

        Args:
            documents: LangChain文档列表
            doc_id: 文档ID
            file_md5: 文件MD5值

        Returns:
            插入的切片数量

        Raises:
            RuntimeError: 当insert返回失败码时抛出，包含服务端错误信息
        """
        if not documents:
            logger.warning("文档列表为空，跳过插入")
            return 0

        total = len(documents)
        logger.info("开始插入文档到向量库, doc_id={}, file_md5={}, chunks={}, batch_size={}",
                    doc_id, file_md5, total, VectorConstants.BATCH_SIZE)

        try:
            start_time = time.time()
            inserted_count = 0

            for batch_start in range(0, total, VectorConstants.BATCH_SIZE):
                batch_end = min(batch_start + VectorConstants.BATCH_SIZE, total)
                batch = documents[batch_start:batch_end]

                logger.info("处理分片批次: [{}-{}), 批次大小={}", batch_start, batch_end, len(batch))

                # 1. 批量计算Embedding向量（DashScope text-embedding-v4 单次最多25条）
                #    食物数据：page_content仅存食物名（如"牛蛙"），直接嵌入，不加document:前缀，
                #    否则9个英文字符会淹没2个中文字符的语义
                #    普通文档：保留document:前缀
                texts = [
                    doc.page_content if doc.metadata.get("nutrition_text")
                    else f"{VectorConstants.DOCUMENT_PREFIX}{doc.page_content}"
                    for doc in batch
                ]

                embed_start = time.time()
                batch_embeddings = []
                for embed_start_idx in range(0, len(texts), VectorConstants.EMBEDDING_BATCH_SIZE):
                    embed_end_idx = min(embed_start_idx + VectorConstants.EMBEDDING_BATCH_SIZE, len(texts))
                    embed_batch = texts[embed_start_idx:embed_end_idx]
                    vectors = self.embeddings.embed_documents(embed_batch)
                    batch_embeddings.extend(vectors)
                embed_cost = (time.time() - embed_start) * 1000
                logger.info("嵌入耗时: batch=[{}-{}], count={}, embed_batch_size={}, cost={:.1f}ms",
                            batch_start, batch_end, len(batch), VectorConstants.EMBEDDING_BATCH_SIZE, embed_cost)

                # 2. 构建本批次的Doc列表（官方格式：Doc(id, vector, fields)）
                docs_to_insert = []
                for i, (doc, vec) in enumerate(zip(batch, batch_embeddings)):
                    chunk_index = batch_start + i
                    chunk_id = f"{doc_id}_chunk_{chunk_index}"

                    # 食物数据：page_content仅存食物名(用于embedding)，完整营养信息存metadata
                    # 向量库text字段存完整营养文本(供检索返回展示)，而非嵌入用的带前缀文本
                    stored_text = doc.metadata.get("nutrition_text") or texts[i]

                    docs_to_insert.append(
                        Doc(
                            id=chunk_id,
                            vector=vec,
                            fields={
                                "text": stored_text,
                                "doc_id": doc_id,
                                "file_md5": file_md5,
                                "chunk_index": str(chunk_index),
                                "filename": doc.metadata.get("filename", "")
                            }
                        )
                    )

                # 3. 调用collection.insert批量插入，获取DashVectorResponse
                insert_start = time.time()
                ret = self.collection.insert(docs_to_insert)
                insert_cost = (time.time() - insert_start) * 1000

                # 4. 校验返回值（官方文档：code=0表示成功，assert ret可快速判断）
                if not ret:
                    error_msg = f"插入失败: code={ret.code}, message={ret.message}"
                    logger.error("批次插入失败: batch=[{}-{}], {}", batch_start, batch_end, error_msg)
                    raise RuntimeError(error_msg)

                # 5. 记录usage（Serverless实例返回实际消耗的写请求单元数）
                usage_info = ""
                if hasattr(ret, 'usage') and ret.usage:
                    usage_info = f", usage={ret.usage}"

                logger.info("批次插入成功: batch=[{}-{}], count={}, cost={:.1f}ms{}, request_id={}",
                            batch_start, batch_end, len(docs_to_insert), insert_cost,
                            usage_info, getattr(ret, 'request_id', 'N/A'))

                inserted_count += len(docs_to_insert)

            total_cost = (time.time() - start_time) * 1000
            logger.info("文档插入完成, doc_id={}, inserted={}, total_cost={:.1f}ms",
                        doc_id, inserted_count, total_cost)
            return inserted_count

        except RuntimeError:
            raise
        except Exception as e:
            logger.error("文档插入异常, doc_id={}, error={}", doc_id, str(e))
            raise

    def update_documents(self, documents: List[Document], doc_id: str, file_md5: str = "") -> int:
        """
        更新文档（先删除旧分片，再插入全新切片）

        Args:
            documents: 新的文档切片列表
            doc_id: 文档ID
            file_md5: 文件MD5

        Returns:
            新插入的切片数量
        """
        logger.info("开始更新文档, doc_id={}, new_chunks={}", doc_id, len(documents))

        # 1. 先删除旧分片
        deleted_count = self.delete_by_doc_id(doc_id)
        logger.info("旧分片已删除, doc_id={}, deleted={}", doc_id, deleted_count)

        # 2. 再插入新分片
        inserted_count = self.insert_documents(documents, doc_id, file_md5)
        logger.info("新分片已插入, doc_id={}, inserted={}", doc_id, inserted_count)

        return inserted_count

    def delete_by_doc_id(self, doc_id: str) -> int:
        """
        根据doc_id分页删除所有相关向量
        使用分页方式，避免一次性拉取过大结果集
        使用参数式过滤 + 安全字符串拼接双保险

        Args:
            doc_id: 文档ID

        Returns:
            删除的切片数量
        """
        logger.info("开始分页删除向量, doc_id={}", doc_id)

        if not self._validate_doc_id(doc_id):
            logger.warning("doc_id无效，跳过删除")
            return 0

        total_deleted = 0

        try:
            while True:
                filter_params = {"doc_id": doc_id}
                results = self._query_with_filter(filter_params, topk=VectorConstants.DELETE_PAGE_SIZE)

                if not results or len(results) == 0:
                    break

                # 批量删除当前页，校验返回值
                doc_ids_to_delete = [doc.id for doc in results]
                ret = self.collection.delete(doc_ids_to_delete)

                if not ret:
                    logger.error("批量删除失败: doc_id={}, code={}, message={}",
                                 doc_id, ret.code, ret.message)
                    break

                total_deleted += len(doc_ids_to_delete)

                logger.info("分页删除进度: doc_id={}, 本页删除={}, 总计已删除={}, request_id={}",
                            doc_id, len(doc_ids_to_delete), total_deleted,
                            getattr(ret, 'request_id', 'N/A'))

                # 如果本页数量小于page_size，说明已删除完毕
                if len(results) < VectorConstants.DELETE_PAGE_SIZE:
                    break

            logger.info("向量删除完成, doc_id={}, deleted={}", doc_id, total_deleted)
            return total_deleted

        except Exception as e:
            logger.error("向量删除失败, doc_id={}, error={}", doc_id, str(e))
            raise

    def check_md5_exists(self, file_md5: str) -> bool:
        """
        兜底防重复校验：检索向量库metadata中是否已存在相同file_md5
        使用参数式过滤（dict）+ 安全字符串拼接双保险

        Args:
            file_md5: 文件MD5值

        Returns:
            是否已存在
        """
        if not file_md5:
            return False

        try:
            filter_params = {"file_md5": file_md5}
            results = self._query_with_filter(filter_params, topk=1)

            exists = len(results) > 0
            if exists:
                logger.warning("向量库兜底查重：file_md5={} 已存在", file_md5)
            return exists

        except Exception as e:
            logger.error("向量库MD5查重失败: file_md5={}, error={}", file_md5, str(e))
            return False

    def get_collection_stats(self) -> dict:
        """获取集合统计信息"""
        try:
            stats = self.collection.stats()
            stats_data = {}
            if hasattr(stats, 'to_dict'):
                temp = stats.to_dict()
                if temp is not None:
                    stats_data = temp

            return {
                "collection_name": settings.vector_collection_name,
                "total_docs": stats_data.get("total_doc_count", stats_data.get("total", 0))
            }
        except Exception as e:
            logger.error("获取集合统计失败: {}", str(e))
            return {"error": str(e)}


# -------------------------- 懒加载单例入口 --------------------------
__instance: Optional["VectorService"] = None


def get_vector_service() -> VectorService:
    """获取VectorService单例实例（首次调用初始化，后续复用）"""
    global __instance
    if __instance is None:
        __instance = VectorService()
    return __instance