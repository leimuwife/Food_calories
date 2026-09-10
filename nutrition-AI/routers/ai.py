"""AI服务路由 - 食材热量估算 + 营养师同步对话"""
import json

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


# ==================== 营养师同步对话 ====================

class ChatRequest(BaseModel):
    """营养师对话请求参数（同步接口，供Java后端非流式调用）"""
    message: str = Field(..., description="用户提问内容", min_length=1)
    session_id: str = Field("", description="会话ID；为空表示新建对话，由Java雪花算法生成正式会话后回传")
    user_id: str = Field("", description="用户ID；新建会话（session_id为空）时必传")


def _parse_sse_event(chunk: str):
    """
    解析ReActAgent产出的单个SSE事件块
    格式："event: answer\ndata: {...}\n\n"
    Returns: (事件名, data字典)
    """
    event_name = None
    data: dict = {}
    for line in chunk.strip().splitlines():
        line = line.strip()
        if line.startswith("event:"):
            event_name = line[len("event:"):].strip()
        elif line.startswith("data:"):
            payload = line[len("data:"):].strip()
            try:
                parsed = json.loads(payload)
                if isinstance(parsed, dict):
                    data = parsed
            except json.JSONDecodeError:
                logger.warning("SSE事件data解析失败: payload={}", payload[:200])
    return event_name, data


@router.post("/chat", dependencies=[Depends(verify_api_key)])
async def chat(req: ChatRequest) -> ApiResponse:
    """
    营养师同步对话接口

    内部复用ReActAgent流式引擎（execute_stream），逐块消费SSE事件流，
    收集 answer 事件的最终回答文本后一次性返回，适配Java端同步RestTemplate调用。

    会话ID规则（一次对话只生成一次，由系统生成、前端持有）：
    - 请求带 session_id：视为继续已有对话，直接走Redis/MySQL多轮上下文
    - 请求不带 session_id（新建对话）：必须带 user_id，回调Java雪花算法
      生成正式会话ID并初始化Redis，再执行对话，生成的session_id随响应回传，
      前端后续消息必须携带该ID
    """
    # 延迟导入避免模块加载顺序问题
    from Agent.react_agent import get_react_agent
    from services.session_service import get_session_service, SessionServiceException

    session_id = (req.session_id or "").strip()
    user_id = (req.user_id or "").strip()
    is_new_session = not session_id

    # 新建对话：回调Java生成正式雪花会话（写MySQL + 初始化Redis），不使用临时会话
    if is_new_session:
        if not user_id:
            logger.warning("新建对话缺少user_id，无法创建会话")
            return error_response("新建会话缺少用户身份信息（user_id）", ErrorCode.BAD_REQUEST)
        try:
            session_id = get_session_service().create_session(user_id)
            logger.info("新对话已创建正式会话: session_id={}, user_id={}", session_id, user_id)
        except SessionServiceException as e:
            logger.warning("创建会话失败: user_id={}, error={}", user_id, str(e))
            return error_response(f"创建会话失败: {str(e)}", ErrorCode.INTERNAL_ERROR)
        except Exception as e:
            logger.exception("创建会话异常: user_id={}, error={}", user_id, str(e))
            return error_response("创建会话失败，请稍后重试", ErrorCode.INTERNAL_ERROR)

    logger.info("营养师同步对话请求: session_id={}, isNew={}, message={}",
                session_id, is_new_session, req.message[:50])

    try:
        agent = get_react_agent()
        answer_parts = []
        error_msg = None

        async for chunk in agent.execute_stream(session_id, req.message):
            event_name, data = _parse_sse_event(chunk)
            if event_name == "answer":
                answer_parts.append(str(data.get("content", "")))
            elif event_name == "error":
                error_msg = str(data.get("message", "对话服务异常，请稍后重试"))

        answer = "".join(answer_parts).strip()

        # 有错误且未产出任何回答 → 判定失败
        if not answer:
            msg = error_msg or "AI未生成有效回答，请稍后重试"
            logger.warning("营养师对话失败: session_id={}, error={}", session_id, msg)
            return error_response(msg, ErrorCode.INTERNAL_ERROR)

        logger.info("营养师同步对话成功: session_id={}, isNew={}, answer_len={}",
                    session_id, is_new_session, len(answer))
        return success_response(
            {"response": answer, "session_id": session_id, "is_new_session": is_new_session},
            "对话成功"
        )
    except Exception as e:
        logger.exception("营养师同步对话异常: session_id={}, error={}", session_id, str(e))
        return error_response(f"对话失败: {str(e)}", ErrorCode.INTERNAL_ERROR)
