"""工具二：query_food_nutrition - 食物营养数据查询

功能：接收大模型提供的食物名称列表，调用RAG检索服务从食物知识库中
逐个查询对应食物的营养数据（热量、蛋白质、脂肪、碳水），
返回纯文本清单供大模型进行饮食搭配推荐。

设计约束：
- 与session_service完全解耦，不引入任何会话相关代码
- 调用已封装的SearchService执行DashVector向量检索
- 向量库沿用优化后的入库策略：仅靠纯食物名称生成向量，
  DashVector返回余弦距离(distance)，值越小越相似
- 工具不做热量过滤和饮食搭配，仅返回原始营养数据，
  由大模型自行判断食物组合、控制总热量
- RAG异常时捕获异常、输出日志，返回简短异常提示文本
"""
from loguru import logger

from constants.global_constants import AgentConstants
from services.search_service import get_search_service

# ---- 工具元信息（注册到tool_registry使用） ----
TOOL_NAME = "query_food_nutrition"
TOOL_DESCRIPTION = (
    "当需要查询具体食物的营养数据时调用本工具。"
    "大模型根据用户需求（如早餐推荐、减脂餐搭配等）自行分析出候选食物名称列表，"
    "将食物名称列表传入本工具，工具从食物知识库中检索出每种食物的热量、蛋白质、脂肪、碳水等营养数据。"
    "大模型拿到营养数据后自行进行食物组合搭配，确保总热量满足用户要求。"
    "重要提示：本工具对每个查询词会返回最多3个候选匹配，"
    "如果最前面的候选与用户意图不符（例如搜'鸡蛋'返回'鸡蛋黄'），"
    "请优先从候选中选择语义正确的条目，或换更精确的名称重新调用工具。"
)
TOOL_PARAMETERS = [
    {"name": "food_names", "type": "list[str]", "required": True,
     "description": "食物名称列表，如 [\"鸡蛋\", \"全麦面包\", \"牛奶\"]"},
]
TOOL_RETURN_FORMAT = "纯文本字符串，每个查询词下返回多条候选（distance越小越相似），候选包含匹配食物名和完整营养数据"

# 每个查询词最多返回 TOP_N_CANDIDATES 个候选，让模型自行挑最符合用户意图的那条
TOP_N_CANDIDATES = 3
# 业务层硬性阈值：超过该距离且字面无匹配的候选直接丢弃（避免完全无关条目污染候选池）
DISTANCE_HARD_CUTOFF = 0.45
# 字面量匹配加分（相当于 distance 减去该值，让正确条目即使 distance 略大也能排前）
EXACT_MATCH_BONUS = 0.15   # 匹配名 == 查询词
CONTAIN_MATCH_BONUS = 0.08  # 匹配名包含查询词 或 查询词包含匹配名
# distance 超过该值提示模型"可能匹配不准确，谨慎使用或换词再查"
DISTANCE_WARN_THRESHOLD = 0.10


def _extract_matched_food_name(nutrition_text: str) -> str:
    """
    从知识库保存的nutrition_text中提取"命中的真实食物名称"
    nutrition_text 开头通常是："食物名称：鸡蛋黄，分类：..." 或 "鸡蛋黄，分类..."
    取第一个分号/冒号后、第一个逗号/句号前的内容作为展示用匹配名；
    提取不到时直接返回文本前20个字符作为兜底展示。
    """
    if not nutrition_text:
        return ""

    head = nutrition_text.strip()
    # 处理 "食物名称：XXX，..." 格式（新版content字段）
    if "食物名称：" in head or "食物名称:" in head:
        marker = "食物名称：" if "食物名称：" in head else "食物名称:"
        tail = head.split(marker, 1)[1]
        for sep in ("，", ",", "。", ".", "\n"):
            if sep in tail:
                return tail.split(sep, 1)[0].strip()
        return tail.strip()

    # 否则取第一个逗号/句号前的内容
    for sep in ("，", ",", "。", ".", "\n"):
        if sep in head:
            name = head.split(sep, 1)[0].strip()
            if name:
                return name

    return head[:20].strip()


def _rerank_with_literal_match(query: str, candidates: list) -> list:
    """
    业务语义重排：结合字面量匹配对DashVector向量距离结果做二次排序。

    向量距离在"短名称 + 括号后缀"场景下区分度不够（例如query="鸡蛋" vs
    "鸡蛋（红皮）"只字面上差了后缀，但向量距离可能高于"鸡蛋黄"这种字面更接近的错误条目）。
    通过字面量加分让"名称语义匹配"的条目排在前面。

    重排规则（在原始distance基础上做扣除，扣除后排升序即可）：
    1. 匹配名 完全等于 查询词（忽略空格/括号） → 扣 EXACT_MATCH_BONUS
    2. 匹配名 包含 查询词（含括号变形） → 扣 CONTAIN_MATCH_BONUS
    3. 查询词 包含 匹配名 → 扣 CONTAIN_MATCH_BONUS / 2

    Args:
        query: 用户查询的食物名
        candidates: search_service返回的候选列表（每项含 text, score, ...）

    Returns:
        list[dict]：每条追加 adjusted_score、matched_name、match_type
    """
    normalized_query = query.strip().replace(" ", "")
    enriched = []

    for cand in candidates:
        score = cand.get("score", 1.0)
        matched_name = _extract_matched_food_name(cand.get("text", ""))
        normalized_name = matched_name.replace(" ", "")

        match_type = "纯向量"
        bonus = 0.0
        if normalized_name and normalized_query:
            if normalized_name == normalized_query:
                match_type = "完全匹配"
                bonus = EXACT_MATCH_BONUS
            elif normalized_query in normalized_name:
                match_type = "名称包含查询词"
                bonus = CONTAIN_MATCH_BONUS
            elif normalized_name in normalized_query:
                match_type = "查询词包含名称"
                bonus = CONTAIN_MATCH_BONUS / 2

        adjusted = max(0.0, score - bonus)
        enriched.append({
            **cand,
            "matched_name": matched_name,
            "match_type": match_type,
            "adjusted_score": adjusted,
        })

    # 按调整后距离升序排；若完全相同，再按匹配类型优先级
    MATCH_RANK = {"完全匹配": 0, "名称包含查询词": 1, "查询词包含名称": 2, "纯向量": 3}
    enriched.sort(key=lambda c: (c["adjusted_score"], MATCH_RANK.get(c["match_type"], 99), c["score"]))

    # 最后裁掉距离实在太大且无任何字面匹配的条目
    def _keep(cand_: dict) -> bool:
        if cand_["score"] <= DISTANCE_HARD_CUTOFF:
            return True
        return cand_["match_type"] != "纯向量"

    return [c for c in enriched if _keep(c)]


def query_food_nutrition(food_names: list) -> str:
    """
    根据食物名称列表批量查询营养数据

    每个查询词返回最多 TOP_N_CANDIDATES 个候选匹配（distance升序），
    让模型自己选择最贴近用户意图的那条，而不是盲目取top1造成语义错位。

    Args:
        food_names: 食物名称列表，如 ["鸡蛋", "全麦面包", "牛奶"]

    Returns:
        通俗易懂的中文字符串，每个查询词下最多3条候选，每条标注distance和匹配食物名
    """
    if not food_names or not isinstance(food_names, list):
        return "请提供食物名称列表后重试。"

    logger.info("食物营养查询: food_names={}", food_names)

    try:
        search_service = get_search_service()
    except Exception as e:
        logger.error("query_food_nutrition 检索服务初始化异常: error={}", str(e))
        return AgentConstants.MSG_RAG_ERROR

    lines = ["以下是您查询的食物营养候选数据（每100g含量）：",
             "（distance值越小表示与查询词向量越相似；若候选名称与用户意图不符，请从候选中挑选或换更精确名称重新查询）",
             ""]

    queried_count = 0
    matched_candidates = 0

    for food_name in food_names:
        if not food_name or not isinstance(food_name, str):
            continue
        food_name = food_name.strip()
        if not food_name:
            continue
        queried_count += 1

        try:
            results = search_service.search(food_name, topk=AgentConstants.FOOD_SEARCH_TOPK)
        except Exception as e:
            logger.error("query_food_nutrition RAG检索异常: food_name={}, error={}", food_name, str(e))
            lines.append(f"【查询词：{food_name}】")
            lines.append(f"- 检索异常：{AgentConstants.MSG_RAG_ERROR}")
            lines.append("")
            continue

        if not results:
            logger.info("query_food_nutrition 检索无结果: food_name={}", food_name)
            lines.append(f"【查询词：{food_name}】")
            lines.append("- 知识库中未找到该食物的营养数据，建议换更精确的食物名称重试")
            lines.append("")
            continue

        # 业务语义重排：结合字面量匹配对向量结果做二次排序，
        # 解决"鸡蛋"→"鸡蛋黄"先于"鸡蛋（红皮）"这类问题
        reranked = _rerank_with_literal_match(food_name, results)

        if not reranked:
            logger.info("query_food_nutrition 重排后无候选: food_name={}", food_name)
            lines.append(f"【查询词：{food_name}】")
            lines.append("- 知识库中未找到合适的匹配（建议换更精确的食物名称，如「鸡蛋（全蛋）」）")
            lines.append("")
            continue

        candidates = reranked[:TOP_N_CANDIDATES]
        lines.append(f"【查询词：{food_name}】共 {len(candidates)} 个候选（已按名称语义重排）：")

        for rank, cand in enumerate(candidates, 1):
            nutrition_text = cand.get("text", "")
            raw_score = cand.get("score", 1.0)
            adjusted = cand.get("adjusted_score", raw_score)
            matched_name = cand.get("matched_name", "")
            match_type = cand.get("match_type", "纯向量")

            warn_parts = []
            if raw_score > DISTANCE_WARN_THRESHOLD:
                warn_parts.append(f"距离{raw_score:.4f}较大")
            if match_type == "纯向量" and raw_score > DISTANCE_WARN_THRESHOLD:
                warn_parts.append("匹配可能不准确，谨慎使用或换词再查")
            warn = f"（{'，'.join(warn_parts)}）" if warn_parts else ""

            logger.info(
                "query_food_nutrition 候选#{}/{}: query={}, matched={}, match_type={}, "
                "raw_score={:.4f}, adjusted={:.4f}, text_preview={}",
                rank, len(candidates), food_name, matched_name, match_type,
                raw_score, adjusted, nutrition_text[:60]
            )

            lines.append(
                f"- 候选{rank} | 匹配名称：{matched_name} | 匹配类型：{match_type} | "
                f"向量距离：{raw_score:.4f}（重排后{adjusted:.4f}）{warn}"
            )
            lines.append(f"  营养详情：{nutrition_text}")
            matched_candidates += 1

        lines.append("")

    if queried_count == 0:
        return AgentConstants.MSG_NO_FOOD_MATCH

    lines.append("说明：请根据'匹配名称'和距离从候选中挑选最符合用户意图的食物数据，不要机械使用第一条。")
    lines.append("")

    logger.info(
        "query_food_nutrition 完成: queried={}, matched_candidates={}",
        queried_count, matched_candidates
    )
    return "\n".join(lines)
