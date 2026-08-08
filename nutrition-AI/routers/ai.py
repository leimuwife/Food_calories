"""AI服务路由 - 食材热量估算"""
from fastapi import APIRouter, Depends
from loguru import logger
from pydantic import BaseModel, Field

from services.calorie_service import get_calorie_service, CalorieEstimateException
from utils.response import success_response, error_response, ErrorCode, ApiResponse
from utils.auth import verify_api_key

router = APIRouter(prefix="/api/ai", tags=["AI服务"])


class CalorieEstimateRequest(BaseModel):
    """食材热量估算请求参数"""
    food_name: str = Field(..., description="食物名称", min_length=1)
    food_desc: str = Field("", description="食物补充描述")
    weight: float = Field(..., gt=0, description="食材重量（克）")
    # system_prompt：Java端从MySQL读取的AI配置系统提示词，热量估算不使用（用外部txt），
    # 保留此字段供后续聊天功能复用同一接口结构
    system_prompt: str = Field("", description="系统提示词（热量估算不使用，预留聊天功能）")


@router.post("/estimate-calorie", dependencies=[Depends(verify_api_key)])
async def estimate_calorie(req: CalorieEstimateRequest) -> ApiResponse:
    """
    食材热量估算接口

    链路：参数预处理 → Redis+RAG并行查询 → 数据聚合 → 外部Prompt → 大模型计算 → 结构化输出
    入参：food_name（食物名称）、food_desc（补充描述）、weight（重量，单位g）
    注意：热量估算使用 config/prompts/calorie_estimate.txt 提示词，不使用 system_prompt 参数
    """
    logger.info("热量估算请求: food_name={}, food_desc={}, weight={}",
                req.food_name, req.food_desc, req.weight)

    try:
        service = get_calorie_service()
        # system_prompt 传入但热量估算链路不使用（用外部txt提示词）
        result = service.estimate(req.food_name, req.food_desc, req.weight, req.system_prompt)
        return success_response(result, "热量估算成功")
    except CalorieEstimateException as e:
        logger.warning("热量估算业务失败: error={}", str(e))
        return error_response(str(e), ErrorCode.BAD_REQUEST)
    except Exception as e:
        logger.exception("热量估算异常: error={}", str(e))
        return error_response(f"热量估算失败: {str(e)}", ErrorCode.INTERNAL_ERROR)
