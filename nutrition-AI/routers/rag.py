"""RAG知识库路由"""
from fastapi import APIRouter, UploadFile, File, Form, Depends, Query
from loguru import logger
from typing import Optional

from services.vector_service import get_vector_service
from utils.response import success_response, error_response, ErrorCode, ApiResponse
from utils.auth import verify_api_key
from utils.md5 import calculate_md5, verify_md5
from services.document_service import DocumentService, SUPPORTED_FORMATS
from services.callback_service import CallbackService
from config.settings import settings
from constants.global_constants import VectorConstants, CommonConstants

router = APIRouter(prefix="/api/rag", tags=["RAG知识库管理"])

doc_router = APIRouter(prefix="/document", tags=["文档管理"])
search_router = APIRouter(prefix="/search", tags=["知识库检索"])

document_service = DocumentService()
callback_service = CallbackService()


@doc_router.post("/upload", dependencies=[Depends(verify_api_key)])
async def upload_document(
    file: UploadFile = File(..., description="上传的文档文件"),
    doc_id: str = Form(..., description="文档ID（MySQL主键）"),
    file_md5: str = Form(..., description="Java端已计算的文件MD5")
) -> ApiResponse:
    """
    知识库文档入库接口

    流程：
    1. 参数校验：doc_id非空、文件格式
    2. 读取文件二进制，本地重新计算MD5，与Java传递的MD5比对
    3. 向量库file_md5兜底查重
    4. 文档解析+切片
    5. 分批向量化入库（每50条一批）
    6. 回调Java通知结果
    """
    # 获取向量服务实例（懒加载）
    vec_service = get_vector_service()

    # ========== 参数校验 ==========
    if not doc_id or not doc_id.strip():
        return error_response("doc_id不能为空", ErrorCode.BAD_REQUEST)

    logger.info("收到文档入库请求: doc_id={}, filename={}", doc_id, file.filename)

    try:
        # 1. 校验文件格式
        if not file.filename:
            return error_response("文件名为空", ErrorCode.FILE_FORMAT_ERROR)

        if not document_service.validate_file_format(file.filename):
            return error_response(
                f"不支持的文件格式，支持的格式: {', '.join(SUPPORTED_FORMATS)}",
                ErrorCode.FILE_FORMAT_ERROR
            )

        # 2. 读取文件内容
        file_content = await file.read()

        if not file_content:
            return error_response("文件内容为空", ErrorCode.FILE_EMPTY_ERROR)

        # 3. MD5校验
        local_md5 = calculate_md5(file_content)
        if not verify_md5(file_content, file_md5):
            error_msg = f"文件MD5校验失败，文件可能传输损坏或遭篡改: local={local_md5}, remote={file_md5}"
            logger.error(error_msg)
            await callback_service.notify_upload_failure(doc_id, error_msg)
            return error_response(error_msg, ErrorCode.FILE_PARSE_ERROR)

        logger.info("MD5校验通过: doc_id={}, file_md5={}", doc_id, file_md5)

        # 4. 向量库file_md5兜底查重
        if vec_service.check_md5_exists(file_md5):
            error_msg = f"向量库兜底查重：file_md5={file_md5} 已存在，疑似重复上传"
            logger.warning(error_msg)
            await callback_service.notify_upload_failure(doc_id, error_msg)
            return error_response(error_msg, ErrorCode.FILE_FORMAT_ERROR)

        # 5. 处理文档（解析+切片）
        chunks, chunk_count = document_service.process_document(file_content, file.filename)
        logger.info("文档处理完成: doc_id={}, chunks={}", doc_id, chunk_count)

        if chunk_count == 0:
            return error_response("文档切片数量为0，可能内容无效", ErrorCode.FILE_EMPTY_ERROR)

        # 6. 分批插入向量
        inserted_count = vec_service.insert_documents(chunks, doc_id, file_md5)
        logger.info("文档入库完成: doc_id={}, inserted={}", doc_id, inserted_count)

        # 7. 回调Java通知成功
        callback_result = await callback_service.notify_upload_success(
            doc_id=doc_id,
            collection_id=settings.vector_collection_name,
            chunk_count=inserted_count
        )

        result_data = {
            "doc_id": doc_id,
            "file_md5": file_md5,
            "chunk_count": chunk_count,
            "inserted_count": inserted_count,
            "callback_success": callback_result
        }

        return success_response(result_data, "文档入库成功")

    except ValueError as e:
        logger.error("文档处理参数错误: doc_id={}, error={}", doc_id, str(e))
        await callback_service.notify_upload_failure(doc_id, str(e))
        return error_response(str(e), ErrorCode.FILE_PARSE_ERROR)

    except Exception as e:
        logger.exception("文档入库异常: doc_id={}, error={}", doc_id, str(e))
        await callback_service.notify_upload_failure(doc_id, f"服务器内部错误: {str(e)}")
        return error_response(f"文档入库失败: {str(e)}", ErrorCode.INTERNAL_ERROR)


@doc_router.delete("/delete", dependencies=[Depends(verify_api_key)])
async def delete_document(doc_id: str = Query(..., description="文档ID")) -> ApiResponse:
    """
    知识库文档删除接口（分页删除）

    说明：删除流程由Java同步调用本接口，Java通过HTTP响应判断删除结果，
    无需通过回调更新Java端文档状态——避免回调把"已删除"状态覆盖回"正常"。
    """
    # 获取向量服务实例
    vec_service = get_vector_service()

    # ========== 参数校验 ==========
    if not doc_id or not doc_id.strip():
        return error_response("doc_id不能为空", ErrorCode.BAD_REQUEST)

    logger.info("收到文档删除请求: doc_id={}", doc_id)

    try:
        deleted_count = vec_service.delete_by_doc_id(doc_id)

        result_data = {
            "doc_id": doc_id,
            "deleted_count": deleted_count,
        }
        if deleted_count > 0:
            return success_response(result_data, "文档删除成功")
        else:
            result_data["message"] = "未找到对应的向量数据"
            return success_response(result_data, "未找到需要删除的向量")

    except Exception as e:
        logger.exception("文档删除异常: doc_id={}, error={}", doc_id, str(e))
        return error_response(f"文档删除失败: {str(e)}", ErrorCode.VECTOR_DB_ERROR)


@doc_router.post("/update", dependencies=[Depends(verify_api_key)])
async def update_document(
    file: UploadFile = File(..., description="上传的新文档文件"),
    doc_id: str = Form(..., description="文档ID"),
    file_md5: str = Form(..., description="Java端已计算的文件MD5")
) -> ApiResponse:
    """
    知识库文档更新接口（覆盖更新）
    调用 update_documents 方法：先删旧分片，再插入全新切片
    """
    # 获取向量服务实例
    vec_service = get_vector_service()

    # ========== 参数校验 ==========
    if not doc_id or not doc_id.strip():
        return error_response("doc_id不能为空", ErrorCode.BAD_REQUEST)

    logger.info("收到文档更新请求: doc_id={}, filename={}", doc_id, file.filename)

    try:
        # 1. 校验文件格式
        if not file.filename:
            return error_response("文件名为空", ErrorCode.FILE_FORMAT_ERROR)

        if not document_service.validate_file_format(file.filename):
            return error_response(
                f"不支持的文件格式，支持的格式: {', '.join(SUPPORTED_FORMATS)}",
                ErrorCode.FILE_FORMAT_ERROR
            )

        # 2. 读取文件内容
        file_content = await file.read()

        if not file_content:
            return error_response("文件内容为空", ErrorCode.FILE_EMPTY_ERROR)

        # 3. MD5校验
        local_md5 = calculate_md5(file_content)
        if not verify_md5(file_content, file_md5):
            error_msg = f"文件MD5校验失败: local={local_md5}, remote={file_md5}"
            logger.error(error_msg)
            await callback_service.notify_upload_failure(doc_id, error_msg)
            return error_response(error_msg, ErrorCode.FILE_PARSE_ERROR)

        # 4. 处理新文档
        chunks, chunk_count = document_service.process_document(file_content, file.filename)
        logger.info("新文档处理完成: doc_id={}, chunks={}", doc_id, chunk_count)

        if chunk_count == 0:
            return error_response("文档切片数量为0，可能内容无效", ErrorCode.FILE_EMPTY_ERROR)

        # 5. 使用 update_documents：先删旧分片，再插入全新切片
        inserted_count = vec_service.update_documents(chunks, doc_id, file_md5)

        # 6. 回调Java通知成功
        callback_result = await callback_service.notify_update_success(doc_id, inserted_count)

        result_data = {
            "doc_id": doc_id,
            "file_md5": file_md5,
            "new_chunk_count": chunk_count,
            "new_inserted_count": inserted_count,
            "callback_success": callback_result
        }

        return success_response(result_data, "文档更新成功")

    except ValueError as e:
        logger.error("文档处理参数错误: doc_id={}, error={}", doc_id, str(e))
        await callback_service.notify_upload_failure(doc_id, str(e))
        return error_response(str(e), ErrorCode.FILE_PARSE_ERROR)

    except Exception as e:
        logger.exception("文档更新异常: doc_id={}, error={}", doc_id, str(e))
        await callback_service.notify_upload_failure(doc_id, f"服务器内部错误: {str(e)}")
        return error_response(f"文档更新失败: {str(e)}", ErrorCode.INTERNAL_ERROR)


@search_router.post("/query", dependencies=[Depends(verify_api_key)])
async def search_knowledge(
    query: str = Form(..., description="查询文本"),
    topk: int = Form(VectorConstants.DEFAULT_SEARCH_TOPK, description="返回结果数量（1-20）")
) -> ApiResponse:
    """
    知识库问答检索接口

    增加参数校验：topk合法区间1-20
    增加score >= 0.75阈值过滤
    """
    # 获取向量服务实例
    vec_service = get_vector_service()

    # ========== 参数校验 ==========
    if not query or not query.strip():
        return error_response("查询内容不能为空", ErrorCode.BAD_REQUEST)

    # topk校验：限制在合法区间
    if topk < VectorConstants.MIN_SEARCH_TOPK or topk > VectorConstants.MAX_SEARCH_TOPK:
        logger.warning("topk超出合法区间: topk={}, 限制为{}-{}",
                       topk, VectorConstants.MIN_SEARCH_TOPK, VectorConstants.MAX_SEARCH_TOPK)
        topk = max(VectorConstants.MIN_SEARCH_TOPK, min(topk, VectorConstants.MAX_SEARCH_TOPK))

    logger.info("收到知识库检索请求: query={}, topk={}", query[:50], topk)

    try:
        results = vec_service.search(query, topk)

        result_data = {
            "query": query,
            "topk": topk,
            "total": len(results),
            "results": results
        }

        return success_response(result_data, "检索成功")

    except Exception as e:
        logger.exception("知识库检索异常: query={}, error={}", query[:50], str(e))
        return error_response(f"检索失败: {str(e)}", ErrorCode.VECTOR_DB_ERROR)


@doc_router.get("/health", dependencies=[Depends(verify_api_key)])
async def health_check() -> ApiResponse:
    """健康检查接口"""
    vec_service = get_vector_service()
    try:
        stats = vec_service.get_collection_stats()
        return success_response({
            "status": "healthy",
            "vector_db": stats
        })
    except Exception as e:
        return error_response(f"健康检查失败: {str(e)}", ErrorCode.INTERNAL_ERROR)


router.include_router(doc_router)
router.include_router(search_router)