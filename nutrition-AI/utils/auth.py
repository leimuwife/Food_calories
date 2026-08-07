"""API鉴权工具"""
from fastapi import Header, HTTPException, Request, status
from loguru import logger
from config.settings import settings


async def verify_api_key(request: Request, authorization: str = Header(None, alias="Authorization")) -> None:
    """
    校验API密钥
    请求头格式: Authorization: Bearer <api_key>
    """
    if not authorization:
        logger.warning("请求缺少Authorization头, 客户端IP: {}", request.client.host if request.client else "unknown")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"code": 401, "msg": "缺少认证令牌"}
        )

    # 解析Bearer Token
    parts = authorization.split(" ")
    if len(parts) != 2 or parts[0].lower() != "bearer":
        logger.warning("Authorization头格式错误, 客户端IP: {}", request.client.host if request.client else "unknown")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"code": 401, "msg": "认证令牌格式错误"}
        )

    token = parts[1].strip()
    expected_key = settings.api_secret_key

    if token != expected_key:
        logger.warning("API密钥校验失败, 客户端IP: {}", request.client.host if request.client else "unknown")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"code": 401, "msg": "API密钥无效"}
        )

    logger.debug("API密钥校验通过, 客户端IP: {}", request.client.host if request.client else "unknown")


def create_auth_header() -> dict:
    """构造鉴权请求头（用于回调Java）"""
    return {
        "Authorization": f"Bearer {settings.api_secret_key}"
    }
