"""ReAct-Agent 大模型对话编排引擎

对外唯一入口：ReActAgent.execute_stream(session_id, user_query)
返回 SSE 格式流式数据，适配 Vue 前端流式展示。

"""
import json
from typing import Any, AsyncGenerator, Dict, List, Optional

from loguru import logger
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage

from constants.global_constants import AgentConstants, ChatConstants
from Agent.tools.tool_registry import TOOL_REGISTRY, build_tools_prompt, call_tool


class ReActAgent:
    """ReAct（思考-行动-观察）循环对话引擎"""

    def __init__(self, model, session_service, tools: Optional[Dict[str, Dict[str, Any]]] = None) -> None:
        # 依赖注入三要素
        self.model = model
        self.session_service = session_service
        self.tools = tools if tools is not None else TOOL_REGISTRY

        self.max_iterations = AgentConstants.MAX_AGENT_ITERATIONS
        logger.info("ReActAgent初始化完成: tools={}", list(self.tools.keys()))

    async def execute_stream(self, session_id: str, user_query: str) -> AsyncGenerator[str, None]:
        """
        流式执行一次对话交互（ReAct循环 + SSE流式输出）

        Args:
            session_id: 会话标识
            user_query: 用户最新提问文本

        Yields:
            SSE格式事件流：
                event: start / thought / tool_call / tool_result / answer / done / error
        """
        try:
            # 1. 读取会话近期上下文（Redis优先，缓存失效自动落盘+回源MySQL）
            history = self.session_service.get_recent_history(session_id)
            logger.info("Agent对话开始: session_id={}, query={}, history_count={}",
                        session_id, user_query[:50], len(history))
            yield self._sse("start", {"sessionId": session_id})

            # 2. 组装初始消息（系统提示词 + 历史 + 当前问题）
            messages = self._build_initial_messages(history, user_query)

            # 3. ReAct循环：思考-工具调用-观察，直到模型判定无需调用工具
            iterations = 0
            while iterations < self.max_iterations:
                iterations += 1

                # 调用模型（工具绑定版本，支持结构化工具调用）
                response = await self._invoke_model(messages)
                if response is None:
                    # 模型调用失败已在上层返回错误，直接结束
                    yield self._sse("error", {"message": AgentConstants.MSG_AGENT_ERROR})
                    yield self._sse("done", {})
                    return

                # 提取思考内容（模型在调用工具前通常会输出推理）
                thought = self._extract_thought(response)
                if thought:
                    self._safe_append(session_id, "ai_thought", thought)
                    yield self._sse("thought", {"content": thought})
                    messages.append(AIMessage(content=thought))

                # 检测是否有工具调用
                tool_calls = self._extract_tool_calls(response)
                if not tool_calls:
                    # 分支A：普通自然语言回答
                    answer = self._extract_answer(response)
                    if not answer:
                        answer = AgentConstants.MSG_AGENT_ERROR
                    self._safe_append(session_id, "ai_answer", answer)
                    yield self._sse("answer", {"content": answer})
                    yield self._sse("done", {})
                    return

                # 分支B：执行工具调用
                for tc in tool_calls:
                    tool_name = tc.get("name")
                    tool_args = tc.get("args", {})

                    # 记录 tool_call 消息
                    tool_call_payload = json.dumps({"name": tool_name, "args": tool_args}, ensure_ascii=False)
                    self._safe_append(session_id, "tool_call", tool_call_payload)
                    yield self._sse("tool_call", {"name": tool_name, "args": tool_args})

                    # 执行工具（工具内部捕获异常返回友好文本）
                    tool_result = self._execute_tool(tool_name, tool_args)
                    self._safe_append(session_id, "tool_result", tool_result)
                    yield self._sse("tool_result", {"name": tool_name, "content": tool_result})

                    # 将工具调用结果并入上下文，继续循环
                    # 注意：langchain-core 高版本 AIMessage.content 不允许 None，需用空字符串占位
                    messages.append(AIMessage(content="", tool_calls=[{
                        "name": tool_name,
                        "args": tool_args,
                        "id": tc.get("id", f"call_{iterations}_{tool_name}"),
                    }]))
                    messages.append(ToolMessage(content=tool_result, tool_call_id=tc.get("id", f"call_{iterations}_{tool_name}")))

            # 4. 达到最大轮次，兜底输出
            logger.warning("Agent达到最大工具调用轮次: session_id={}, iterations={}", session_id, iterations)
            fallback = "我已经尽力分析但仍未得出最终结论，建议换个方式描述问题。"
            self._safe_append(session_id, "ai_answer", fallback)
            yield self._sse("answer", {"content": fallback})
            yield self._sse("done", {})

        except Exception as e:
            logger.exception("Agent对话异常: session_id={}, error={}", session_id, str(e))
            yield self._sse("error", {"message": AgentConstants.MSG_AGENT_ERROR})
            yield self._sse("done", {})

    # ----- Prompt组装 -----
    def _build_system_prompt(self) -> str:
        """
        组装系统提示词：角色设定 + 工具介绍 + 工作规范

        角色设定从 Redis（Java端 MySQL ai_config 表的 systemPrompt 字段）读取，
        运营在 Java 管理后台修改提示词后即时生效，无需改代码。
        Redis 未命中时使用默认角色设定兜底。

        工具介绍通过 build_tools_prompt() 动态生成，
        大模型根据工具简介自主判断调用时机，后端不硬编码意图判断。
        """
        # 角色设定：优先从 Redis 读取，读不到用默认值
        from services.redis_service import get_redis_service
        role_prompt = get_redis_service().get_system_prompt()
        if not role_prompt:
            role_prompt = "你是专业的营养健康助手，擅长为用户提供饮食、热量、营养方面的专业建议。"

        tools_intro = build_tools_prompt()
        return (
            f"{role_prompt}\n"
            "你可以使用以下工具来辅助回答：\n"
            f"{tools_intro}\n\n"
            "工作规范：\n"
            "1. 当用户需要查询具体食物的营养数据、计算每日热量目标等场景时，调用对应工具获取准确数据；\n"
            "2. 每次调用工具前先进行思考，基于用户意图决定是否调用工具；\n"
            "3. 工具返回数据后，【必须严格基于工具返回的数据组织回答】，禁止用自己的知识编造、"
            "修正或替换工具返回的营养数值；如工具返回的候选中第一条最匹配用户意图，直接使用该条数据；\n"
            "4. 若工具返回的候选与用户查询词不完全匹配（如搜'鸡蛋'返回'鸡蛋（红皮）'属于正常变形），"
            "应从候选中挑选最贴近用户意图的条目直接使用，不要因此否定工具数据而自行编造；\n"
            "5. 若所有候选均与用户意图差距过大，可换更精确的名称【再次调用工具】（生成tool_calls），"
            "而不是用自己的知识回答；\n"
            "6. 用户未提及食物营养/热量计算的场景不要强行调用工具，直接基于已有知识回答；\n"
            "7. 最终回答要人性化、结构化，方便用户阅读。"
        )

    def _build_initial_messages(self, history: List[Dict[str, str]], user_query: str) -> List:
        """
        组装初始消息列表：系统提示词 + 历史对话 + 当前问题
        历史消息映射规则：
            user → HumanMessage
            ai_answer → AIMessage
            其余（ai_thought/tool_call/tool_result）为过程消息，不载入上下文
        """
        messages: List = [SystemMessage(content=self._build_system_prompt())]

        for item in history:
            role = item.get("role")
            content = item.get("content")
            if not content:
                continue
            if role == "user":
                messages.append(HumanMessage(content=content))
            elif role == "ai_answer":
                messages.append(AIMessage(content=content))

        messages.append(HumanMessage(content=user_query))
        return messages

    # ----- 模型调用 -----
    async def _invoke_model(self, messages: List) -> Optional[AIMessage]:
        """
        调用对话大模型（绑定工具schema，支持结构化工具调用）

        Returns:
            AIMessage；调用失败返回None（已输出错误日志）
        """
        try:
            # 将已注册工具转换为OpenAI function schema
            schemas = [self._tool_to_schema(tool) for tool in self.tools.values()]
            bound_model = self.model.bind_tools(schemas)
            response = await bound_model.ainvoke(messages)
            return response
        except Exception as e:
            logger.exception("Agent模型调用异常: error={}", str(e))
            return None

    @staticmethod
    def _tool_to_schema(tool: Dict[str, Any]) -> Dict[str, Any]:
        """将工具定义转换为OpenAI function calling schema"""
        properties = {}
        required = []
        for param in tool.get("parameters", []):
            name = param["name"]
            prop = {"description": param.get("description", "")}
            ptype = param.get("type", "string")
            if ptype == "list[str]" or ptype.startswith("list"):
                prop["type"] = "array"
                prop["items"] = {"type": "string"}
            elif ptype == "int":
                prop["type"] = "integer"
            elif ptype == "float":
                prop["type"] = "number"
            else:
                prop["type"] = "string"
            properties[name] = prop
            if param.get("required"):
                required.append(name)

        return {
            "type": "function",
            "function": {
                "name": tool["name"],
                "description": tool.get("description", ""),
                "parameters": {
                    "type": "object",
                    "properties": properties,
                    "required": required,
                },
            },
        }

    # ----- 结果解析 -----
    @staticmethod
    def _extract_thought(response: AIMessage) -> str:
        """提取模型思考内容（调用工具前的推理文本）"""
        content = response.content
        if content and isinstance(content, str) and content.strip():
            return content.strip()
        return ""

    @staticmethod
    def _extract_tool_calls(response: AIMessage) -> List[Dict[str, Any]]:
        """提取模型输出的工具调用列表"""
        calls = getattr(response, "tool_calls", None)
        if not calls:
            return []
        result = []
        for call in calls:
            name = call.get("name")
            args = call.get("args") or {}
            result.append({"name": name, "args": args, "id": call.get("id")})
        return result

    @staticmethod
    def _extract_answer(response: AIMessage) -> str:
        """提取最终回答文本"""
        content = response.content
        if isinstance(content, str):
            return content.strip()
        if isinstance(content, list):
            # 多模态/分段内容，仅保留文本部分
            parts = []
            for block in content:
                if isinstance(block, dict) and block.get("type") == "text":
                    parts.append(block.get("text", ""))
            return "".join(parts).strip()
        return ""

    # ----- 工具调度 -----
    def _execute_tool(self, tool_name: str, tool_args: Dict[str, Any]) -> str:
        """
        执行指定工具

        call_tool 内部捕获异常并返回友好文本，不会向上抛出，
        保证工具失败不中断Agent主流程。
        """
        logger.info("Agent执行工具: name={}, args={}", tool_name, tool_args)
        return call_tool(tool_name, **tool_args)

    # ----- 会话消息持久化 -----
    def _safe_append(self, session_id: str, role: str, content: str) -> None:
        """
        安全追加会话消息

        角色枚举严格遵守：user / ai_thought / tool_call / tool_result / ai_answer
        会话服务读写异常不中断Agent主流程，仅记录日志。
        """
        if role not in ChatConstants.VALID_ROLES:
            logger.warning("Agent尝试写入非法角色: role={}", role)
            return
        try:
            self.session_service.append_message(session_id, role, content)
        except Exception as e:
            logger.warning("Agent写入会话消息失败: session_id={}, role={}, error={}",
                           session_id, role, str(e))

    # ----- SSE格式化 -----
    @staticmethod
    def _sse(event: str, data: Any) -> str:
        """格式化SSE事件"""
        data_str = json.dumps(data, ensure_ascii=False)
        return f"event: {event}\ndata: {data_str}\n\n"


__react_agent_instance: Optional[ReActAgent] = None


def get_react_agent(model=None, session_service=None, tools: Optional[Dict[str, Dict[str, Any]]] = None) -> ReActAgent:
    """
    获取ReActAgent单例（依赖注入）

    未显式传入依赖时使用默认：
        model: 从 models 工厂获取对话大模型
        session_service: 会话存储服务单例
        tools: 已注册工具集合
    """
    global __react_agent_instance
    if __react_agent_instance is None or model is not None or session_service is not None:
        if model is None:
            from models import get_llm_model
            model = get_llm_model()
        if session_service is None:
            from services.session_service import get_session_service
            session_service = get_session_service()
        __react_agent_instance = ReActAgent(model=model, session_service=session_service, tools=tools)
    return __react_agent_instance
