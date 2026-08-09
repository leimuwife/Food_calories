"""ReAct Agent工具注册表

职责：
1. 统一管理所有AI可调用工具的元信息（名称、描述、参数、执行函数）
2. 提供工具调用入口（按名称分发到对应函数）
3. 生成ReAct提示词中的工具介绍文本（供大模型自主判断调用时机）

设计约束：
- 后端不硬编码工具调用条件判断，由大模型根据Prompt中的工具描述自主决策
"""
from typing import Any, Dict, List

from loguru import logger

from Agent.tools.calorie_target_tool import (
    TOOL_NAME as CALORIE_TOOL_NAME,
    TOOL_DESCRIPTION as CALORIE_TOOL_DESC,
    TOOL_PARAMETERS as CALORIE_TOOL_PARAMS,
    TOOL_RETURN_FORMAT as CALORIE_TOOL_RETURN,
    calorie_target_suggest,
)
from Agent.tools.recommend_food_tool import (
    TOOL_NAME as RECOMMEND_TOOL_NAME,
    TOOL_DESCRIPTION as RECOMMEND_TOOL_DESC,
    TOOL_PARAMETERS as RECOMMEND_TOOL_PARAMS,
    TOOL_RETURN_FORMAT as RECOMMEND_TOOL_RETURN,
    query_food_nutrition,
)

# ---- 工具注册表 ----
# key = 工具标识名称，value = 工具完整定义
TOOL_REGISTRY: Dict[str, Dict[str, Any]] = {
    CALORIE_TOOL_NAME: {
        "name": CALORIE_TOOL_NAME,
        "description": CALORIE_TOOL_DESC,
        "parameters": CALORIE_TOOL_PARAMS,
        "return_format": CALORIE_TOOL_RETURN,
        "function": calorie_target_suggest,
    },
    RECOMMEND_TOOL_NAME: {
        "name": RECOMMEND_TOOL_NAME,
        "description": RECOMMEND_TOOL_DESC,
        "parameters": RECOMMEND_TOOL_PARAMS,
        "return_format": RECOMMEND_TOOL_RETURN,
        "function": query_food_nutrition,
    },
}


def get_tool_names() -> List[str]:
    """获取所有已注册工具名称列表"""
    return list(TOOL_REGISTRY.keys())


def get_tool(tool_name: str) -> Dict[str, Any]:
    """
    获取指定工具的完整定义

    Args:
        tool_name: 工具标识名称

    Returns:
        工具定义字典；不存在返回None
    """
    return TOOL_REGISTRY.get(tool_name)


def call_tool(tool_name: str, **kwargs) -> str:
    """
    调用指定工具并返回纯文本结果

    Args:
        tool_name: 工具标识名称
        **kwargs: 工具参数（按工具定义的parameters传入）

    Returns:
        工具执行结果（纯文本字符串）

    Raises:
        ValueError: 工具名称不存在
    """
    tool = TOOL_REGISTRY.get(tool_name)
    if tool is None:
        logger.error("工具不存在: {}", tool_name)
        raise ValueError(f"工具不存在: {tool_name}")

    func = tool["function"]
    logger.info("调用工具: name={}, kwargs={}", tool_name, kwargs)

    try:
        result = func(**kwargs)
        logger.info("工具调用完成: name={}, result_len={}", tool_name, len(result) if result else 0)
        return result
    except Exception as e:
        logger.exception("工具调用异常: name={}, error={}", tool_name, str(e))
        return f"工具执行异常: {str(e)}，请稍后重试。"


def build_tools_prompt() -> str:
    """
    生成ReAct提示词中的工具介绍文本

    自动遍历注册表中所有工具，拼装名称、描述、参数说明，
    供大模型自主分析用户问句、按需触发工具调用。

    Returns:
        工具介绍文本（拼入ReAct系统提示词）
    """
    sections = []
    for tool in TOOL_REGISTRY.values():
        param_lines = []
        for param in tool["parameters"]:
            required_tag = "必填" if param["required"] else "选填"
            param_lines.append(
                f"    - {param['name']}（{param['type']}，{required_tag}）：{param['description']}"
            )

        section = (
            f"【工具名称】{tool['name']}\n"
            f"【功能说明】{tool['description']}\n"
            f"【参数说明】\n"
            f"{chr(10).join(param_lines)}\n"
            f"【返回格式】{tool['return_format']}"
        )
        sections.append(section)

    return "\n\n".join(sections)
