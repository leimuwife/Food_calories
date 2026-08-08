"""
营养AI服务 - RAG知识库管理系统
独立Python FastAPI服务，由SpringBoot Java后端调用
功能：知识库文档入库、删除、更新
技术栈：FastAPI + LangChain + 阿里云DashVector + text-embedding-v4
"""
import sys
import os

# 将项目根目录添加到sys.path，确保模块导入正确
project_root = os.path.dirname(os.path.abspath(__file__))
if project_root not in sys.path:
    sys.path.insert(0, project_root)

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from config.settings import settings
from routers.rag import router as rag_router
from routers.ai import router as ai_router
from utils.response import ApiResponse
from services.vector_service import get_vector_service


def setup_logging() -> None:
    """配置日志"""
    logger.remove()
    logger.add(
        sys.stderr,
        level=settings.log_level,
        format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
        colorize=True
    )
    logger.add(
        "logs/app_{time:YYYY-MM-DD}.log",
        level="DEBUG",
        rotation="10 MB",
        retention="30 days",
        compression="zip",
        enqueue=True
    )


def create_app() -> FastAPI:
    """创建FastAPI应用"""
    setup_logging()

    app = FastAPI(
        title="营养AI RAG知识库服务",
        description="独立Python FastAPI服务，提供知识库文档入库、删除、更新功能",
        version="1.0.0",
        docs_url="/docs",
        redoc_url="/redoc"
    )

    # 配置CORS（仅允许内网访问）
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],  # 生产环境应配置具体的Java后端地址
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"]
    )

    # 注册路由
    app.include_router(rag_router)
    app.include_router(ai_router)

    @app.get("/", response_model=ApiResponse)
    async def root() -> ApiResponse:
        """根路径 - 服务信息"""
        return ApiResponse(
            code=200,
            msg="success",
            data={
                "service": "营养AI RAG知识库服务",
                "version": "1.0.0",
                "features": ["文档入库", "文档删除", "文档更新"],
                "vector_db": "阿里云DashVector",
                "embedding": "text-embedding-v4"
            }
        )

    @app.get("/health", response_model=ApiResponse)
    async def health_check() -> ApiResponse:
        """健康检查"""
        return ApiResponse(
            code=200,
            msg="success",
            data={"status": "healthy"}
        )

    @app.on_event("startup")
    async def startup_event():
        """服务启动时初始化"""
        logger.info("=" * 60)
        logger.info("营养AI RAG知识库服务启动中...")
        logger.info("服务配置:")
        logger.info("  - 端口: {}", settings.server_port)
        logger.info("  - 向量库地址: {}", settings.vector_endpoint)
        logger.info("  - 集合名称: {}", settings.vector_collection_name)
        logger.info("  - Embedding模型: {}", settings.embedding_model)
        logger.info("  - 切片配置: chunk_size={}, chunk_overlap={}",
                    settings.chunk_size, settings.chunk_overlap)
        logger.info("  - Java回调地址: {}", settings.java_callback_url)
        logger.info("=" * 60)

        # 使用懒加载函数获取向量服务实例
        try:
            vec_service = get_vector_service()
            stats = vec_service.get_collection_stats()
            logger.info("向量库连接成功: {}", stats)
        except Exception as e:
            logger.warning("向量库初始化失败（将在首次调用时重试）: {}", str(e))

        logger.info("服务启动完成!")

    @app.on_event("shutdown")
    async def shutdown_event():
        """服务关闭时清理"""
        logger.info("营养AI RAG知识库服务关闭中...")
        logger.info("服务已关闭")

    return app


# 创建应用实例
app = create_app()


if __name__ == "__main__":
    import uvicorn

    logger.info("启动营养AI RAG知识库服务...")
    logger.info("访问地址: http://{}:{}", settings.server_host, settings.server_port)
    logger.info("API文档: http://{}:{}/docs", settings.server_host, settings.server_port)

    uvicorn.run(
        app,
        host=settings.server_host,
        port=settings.server_port,
        reload=False,
        log_level=settings.log_level.lower(),
        access_log=True
    )