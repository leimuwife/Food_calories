"""统一响应格式工具"""
from typing import Any, Optional
from pydantic import BaseModel
from constants.global_constants import ErrorCode, CommonConstants


class ApiResponse(BaseModel):
    """统一API响应结构"""
    code: int = ErrorCode.SUCCESS
    msg: str = CommonConstants.SUCCESS_MSG
    data: Optional[Any] = None


def success_response(data: Any = None, msg: str = CommonConstants.SUCCESS_MSG) -> ApiResponse:
    """成功响应"""
    return ApiResponse(code=ErrorCode.SUCCESS, msg=msg, data=data)


def error_response(msg: str, code: int = ErrorCode.INTERNAL_ERROR) -> ApiResponse:
    """错误响应"""
    return ApiResponse(code=code, msg=msg, data=None)
