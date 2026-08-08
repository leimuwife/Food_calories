"""食材热量估算服务 - LangChain LCEL Runnable 链式实现

业务链路：
    参数预处理 → Redis+RAG并行查询 → 数据聚合 → 外部Prompt → 大模型LLM → 结构化输出解析

1. 使用 | 管道符串联 Runnable 组件（RunnablePassthrough / RunnableParallel / RunnableLambda）
2. Redis与RAG两个查询分支通过 RunnableParallel 并行执行，减少接口耗时
3. Prompt 从外部 txt 文件加载（config/prompts/calorie_estimate.txt），业务话术改动无需改代码
4. 单分支失败不阻断链路（仅舍弃失败分支数据），双分支同时失效抛出业务异常
5. 完全复用 SearchService（检索规则、向量库单例、网络重试均保留）
"""
import json
import time
import os
from typing import Any, Dict, List, Optional

from loguru import logger
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableLambda, RunnableParallel, RunnablePassthrough
from langchain_openai import ChatOpenAI

from config.settings import settings
from constants.global_constants import VectorConstants
from services.search_service import get_search_service
from services.redis_service import get_redis_service


class CalorieEstimateException(Exception):
    """热量估算业务异常（双分支同时失效或参数非法）"""


class CalorieEstimateService:
    """食材热量估算服务（LCEL链式）"""

    def __init__(self) -> None:
        # 完全复用既有服务单例（遵循全局单例复用连接硬约束）
        self.search_service = get_search_service()
        self.redis_service = get_redis_service()

        # 读取外置Prompt文件（禁止代码硬编码提示词）
        self.prompt = self._load_prompt()

        # 从Redis读取LLM配置（Java端AiConfigCacheRunner预热，唯一数据源）
        ai_config = self._load_llm_config()

        # 初始化大模型（OpenAI兼容接口）
        self.llm = ChatOpenAI(
            model=ai_config["model"],
            api_key=ai_config["api_key"],
            base_url=ai_config["base_url"],
            temperature=ai_config.get("temperature", 0.1),
            timeout=settings.llm_timeout,
            max_retries=1
        )

        # 构建LCEL链
        self.chain = self._build_chain()

    # ==================== LLM配置加载 ====================

    def _load_llm_config(self) -> Dict[str, Any]:
        """
        从Redis读取Java端预热的AI配置（唯一数据源）

        Java端存储字段为驼峰（modelName/apiUrl/apiKey/temperature），
        此处统一转换为Python风格键名（model/api_key/base_url/temperature）

        Raises:
            CalorieEstimateException: Redis中无AI配置或配置不完整
        """
        try:
            config = self.redis_service.get_ai_config()
            if not config:
                logger.error("Redis中无AI配置，请确保Java端已启动并预热AiConfigCacheRunner")
                raise CalorieEstimateException("AI模型配置未就绪，请稍后重试")

            # 字段映射：Java驼峰 → Python下划线风格
            result = {
                "model": config.get("modelName") or config.get("model_name"),
                "base_url": config.get("apiUrl") or config.get("api_url"),
                "api_key": config.get("apiKey") or config.get("api_key"),
            }

            # 校验必填字段
            missing = [k for k, v in result.items() if not v]
            if missing:
                logger.error("Redis AI配置缺失字段: {}", missing)
                raise CalorieEstimateException("AI模型配置不完整，请检查管理后台配置")

            # temperature可能是字符串/数字（Java BigDecimal序列化），统一转float
            temp = config.get("temperature")
            if temp is not None:
                try:
                    result["temperature"] = float(temp)
                except (TypeError, ValueError):
                    pass

            logger.info("LLM配置从Redis加载成功: model={}, base_url={}",
                        result.get("model"), result.get("base_url"))
            return result

        except CalorieEstimateException:
            raise
        except Exception as e:
            logger.error("Redis加载LLM配置异常: error={}", str(e))
            raise CalorieEstimateException("AI模型配置加载失败，请稍后重试")

    # ==================== Prompt加载 ====================

    def _load_prompt(self) -> ChatPromptTemplate:
        """从外部txt文件加载Prompt模板"""
        prompt_file = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "config", "prompts", "calorie_estimate.txt"
        )
        with open(prompt_file, "r", encoding="utf-8") as f:
            template = f.read()
        logger.info("热量估算Prompt加载完成: file={}, length={}", prompt_file, len(template))
        return ChatPromptTemplate.from_template(template)

    # ==================== 链路各阶段实现 ====================

    def _preprocess(self, inputs: Dict[str, Any]) -> Dict[str, Any]:
        """
        ① 参数预处理与校验

        Args:
            inputs: 原始入参 {food_name, food_desc, weight}

        Returns:
            标准化参数 {food_name, food_desc, weight_g}

        Raises:
            CalorieEstimateException: 参数非法
        """
        food_name = str(inputs.get("food_name") or "").strip()
        food_desc = str(inputs.get("food_desc") or "").strip()
        weight = inputs.get("weight")

        if not food_name:
            raise CalorieEstimateException("食物名称food_name不能为空")
        if weight is None:
            raise CalorieEstimateException("食材重量weight不能为空")
        try:
            weight_g = float(weight)
        except (TypeError, ValueError):
            raise CalorieEstimateException(f"食材重量weight非法: {weight}")
        if weight_g <= 0:
            raise CalorieEstimateException(f"食材重量weight必须大于0: {weight_g}")

        logger.info("热量估算-参数预处理: food_name={}, weight={}g, desc_len={}",
                    food_name, weight_g, len(food_desc))
        return {
            "food_name": food_name,
            "food_desc": food_desc,
            "weight_g": weight_g,
        }

    def _query_redis(self, ctx: Dict[str, Any]) -> Dict[str, Any]:
        """
        ②A 分支A：Redis缓存查询
        以food_name为key查询食材基础热量数据；查不到返回空对象。
        异常单独捕获记录日志，不阻断链路。
        """
        food_name = ctx["food_name"]
        try:
            start = time.time()
            data = self.redis_service.get_food_nutrition(food_name)
            cost = (time.time() - start) * 1000
            hit = data is not None
            logger.info("热量估算-Redis分支: food_name={}, hit={}, cost={:.1f}ms", food_name, hit, cost)
            return {"ok": True, "data": data, "hit": hit, "cost_ms": round(cost, 2)}
        except Exception as e:
            logger.warning("热量估算-Redis分支异常（舍弃该分支）: food_name={}, error={}", food_name, str(e))
            return {"ok": False, "data": None, "hit": False, "error": str(e)}

    def _query_rag(self, ctx: Dict[str, Any]) -> Dict[str, Any]:
        """
        ②B 分支B：RAG知识库检索
        检索关键词为food_name，检索规则完全沿用SearchService（topk约束、阈值、重排截取、网络重试）。
        异常单独捕获记录日志，不阻断链路。
        """
        food_name = ctx["food_name"]
        try:
            start = time.time()
            # 沿用原有检索配置：topk传入合法区间最小值，SearchService内部完成重排截取topk-3
            results = self.search_service.search(food_name, topk=VectorConstants.RETRIEVE_MIN_TOPK)
            cost = (time.time() - start) * 1000
            logger.info("热量估算-RAG分支: food_name={}, recall={}, cost={:.1f}ms",
                        food_name, len(results), cost)
            return {"ok": True, "results": results, "recall": len(results), "cost_ms": round(cost, 2)}
        except Exception as e:
            logger.warning("热量估算-RAG分支异常（舍弃该分支）: food_name={}, error={}", food_name, str(e))
            return {"ok": False, "results": [], "recall": 0, "error": str(e)}

    def _aggregate(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """
        ③ 数据聚合
        整合Redis结果、RAG召回上下文、重量、食物描述，打包为Prompt输入。
        两个分支同时失效 → 抛业务异常直接返回失败提示。
        """
        redis_branch = data.get("redis_data", {})
        rag_branch = data.get("rag_data", {})
        redis_ok = redis_branch.get("ok", False)
        rag_ok = rag_branch.get("ok", False)

        # 双分支同时失效：直接失败
        if not redis_ok and not rag_ok:
            raise CalorieEstimateException("热量数据源均不可用，请稍后重试")

        # 汇总数据来源（供日志与结果溯源）
        sources = []
        if redis_ok and redis_branch.get("hit"):
            sources.append("redis")
        if rag_ok and rag_branch.get("recall", 0) > 0:
            sources.append("rag")

        # Redis数据转JSON文本（无数据给"无"）
        redis_data_text = "无"
        if redis_ok and redis_branch.get("data"):
            redis_data_text = json.dumps(redis_branch["data"], ensure_ascii=False)

        # RAG召回上下文拼接
        rag_results = rag_branch.get("results", []) if rag_ok else []
        if rag_results:
            context_lines = []
            for idx, item in enumerate(rag_results, 1):
                context_lines.append(
                    f"[{idx}] (相似度{item.get('score', 0):.4f}) {item.get('text', '')}"
                )
            rag_context_text = "\n".join(context_lines)
        else:
            rag_context_text = "无"

        logger.info("热量估算-数据聚合: food_name={}, weight={}g, sources={}, rag_recall={}",
                    data["food_name"], data["weight_g"], sources, len(rag_results))

        # 返回Prompt变量 + 透传给最终解析阶段的元数据（references等）
        return {
            # Prompt模板变量
            "redis_data": redis_data_text,
            "rag_context": rag_context_text,
            "food_name": data["food_name"],
            "food_desc": data["food_desc"],
            "weight_g": data["weight_g"],
            # 供最终输出解析使用（Prompt模板会忽略多余字段）
            "references": rag_results,
            "source_hints": sources,
        }

    def _parse_output(self, result: Dict[str, Any]) -> Dict[str, Any]:
        """
        ⑤ 解析LLM输出为标准JSON
        兼容LLM用markdown代码块包裹JSON的情况，并保留原始参考资料。
        """
        meta = result.get("meta", {})
        llm_raw = result.get("llm_out", "")
        # llm_out是LangChain的AIMessage对象，需取.content属性获取纯文本
        llm_text = (llm_raw.content if hasattr(llm_raw, "content") else str(llm_raw)).strip()

        # 去除 ```json ... ``` markdown包裹
        if llm_text.startswith("```"):
            lines = llm_text.strip().splitlines()
            if lines and lines[0].startswith("```"):
                lines = lines[1:]
            if lines and lines[-1].strip() == "```":
                lines = lines[:-1]
            llm_text = "\n".join(lines).strip()

        try:
            parsed = json.loads(llm_text)
            if not isinstance(parsed, dict):
                raise ValueError("LLM输出不是JSON对象")
        except Exception as e:
            logger.error("LLM输出JSON解析失败: error={}, raw={}", e, llm_text[:500])
            raise CalorieEstimateException("大模型输出解析失败，请稍后重试")

        # 数值保留2位小数
        for key in ("total_calorie", "calorie_per_100g", "protein_g", "fat_g", "carbohydrate_g"):
            try:
                parsed[key] = round(float(parsed.get(key, 0)), 2)
            except (TypeError, ValueError):
                parsed[key] = 0.0

        found = bool(parsed.get("found", True))

        result_payload = {
            "food_name": parsed.get("food_name", meta.get("food_name", "")),
            "food_desc": parsed.get("food_desc", meta.get("food_desc", "")),
            "weight_g": meta.get("weight_g", 0),
            "found": found,
            "total_calorie": parsed.get("total_calorie", 0.0),
            "calorie_per_100g": parsed.get("calorie_per_100g", 0.0),
            "protein_g": parsed.get("protein_g", 0.0),
            "fat_g": parsed.get("fat_g", 0.0),
            "carbohydrate_g": parsed.get("carbohydrate_g", 0.0),
            "data_source": parsed.get("data_source", meta.get("source_hints", [])),
            "reason": parsed.get("reason", ""),
            # 原始参考资料（RAG召回结果，供Java端溯源）
            "references": meta.get("references", []),
        }

        logger.info("热量估算-LLM结果: food_name={}, found={}, total={}kcal, source={}, reason={}",
                    result_payload["food_name"], found, result_payload["total_calorie"],
                    result_payload["data_source"], result_payload["reason"])
        return result_payload

    # ==================== LCEL链构建 ====================

    def _build_chain(self) -> RunnablePassthrough:
        """
        使用 | 管道符串联Runnable组件

        链路分层：
            ① 参数预处理 RunnableLambda
            ② Redis+RAG 并行查询 RunnableParallel
            ③ 数据聚合 RunnableLambda
            ④ 外部Prompt + 大模型LLM（子链 prompt | llm）
            ⑤ 结构化输出解析 RunnableLambda
        """
        # ② 并行双查询（Redis分支 + RAG分支），同步执行减少耗时
        parallel_query = RunnableParallel(
            redis_data=RunnableLambda(self._query_redis),
            rag_data=RunnableLambda(self._query_rag),
            # 透传上下文给聚合阶段
            food_name=RunnableLambda(lambda x: x["food_name"]),
            food_desc=RunnableLambda(lambda x: x["food_desc"]),
            weight_g=RunnableLambda(lambda x: x["weight_g"]),
        )

        # ④ Prompt | LLM 子链
        prompt_llm_chain = self.prompt | self.llm

        # 整体链
        chain = (
            RunnableLambda(self._preprocess)        # ① 参数预处理
            | parallel_query                        # ② 并行查询
            | RunnableLambda(self._aggregate)       # ③ 数据聚合
            | RunnableParallel(                     # ④⑤ 并行取LLM结果与透传元数据
                llm_out=prompt_llm_chain,
                meta=RunnableLambda(lambda x: x),
            )
            | RunnableLambda(self._parse_output)    # ⑤ 输出解析
        )
        return chain

    # ==================== 对外入口 ====================

    def estimate(self, food_name: str, food_desc: str, weight: float,
                 system_prompt: str = "") -> Dict[str, Any]:
        """
        食材热量估算入口（同步调用LCEL链）

        Args:
            food_name: 食物名称
            food_desc: 食物补充描述
            weight: 食材重量（克）
            system_prompt: MySQL中的AI配置系统提示词（热量估算不使用，用外部txt；
                           保留参数供后续聊天功能复用接口结构）

        Returns:
            结构化估算结果（总热量/每100g/三大营养素/数据来源/参考资料）

        Raises:
            CalorieEstimateException: 参数非法或双数据源失效
        """
        start = time.time()
        logger.info("热量估算-链路开始: food_name={}, weight={}g", food_name, weight)

        # 热量估算使用外部 calorie_estimate.txt 提示词，不使用 system_prompt
        inputs = {"food_name": food_name, "food_desc": food_desc, "weight": weight}
        result = self.chain.invoke(inputs)

        total_cost = (time.time() - start) * 1000
        logger.info("热量估算-链路完成: food_name={}, total_cost={:.1f}ms", food_name, total_cost)
        return result


# -------------------------- 懒加载单例入口 --------------------------
__calorie_instance: Optional["CalorieEstimateService"] = None


def get_calorie_service() -> CalorieEstimateService:
    """获取CalorieEstimateService单例实例（首次调用初始化，后续复用）"""
    global __calorie_instance
    if __calorie_instance is None:
        __calorie_instance = CalorieEstimateService()
    return __calorie_instance
