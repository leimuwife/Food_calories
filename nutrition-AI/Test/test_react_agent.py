"""ReAct Agent 全链路集成测试

验证：用户提问 → 会话加载 → LLM推理 → 工具调用 → 流式回答 → 会话持久化

前置条件：
1. Java 后端运行中（端口 8088，提供 /api/chat/session/* 回调接口）
2. Redis 运行中（默认 localhost:6379）
3. DashScope API Key 已配置（.env 或环境变量）
4. DashVector 向量库可用（query_food_nutrition 工具依赖）

运行方式：
    cd d:\\JAVA\\project\\nutrition-all\\nutrition-AI
    python -m Test.test_react_agent
"""
import asyncio
import os
import sys

# 将项目根目录加入 sys.path，使测试脚本能导入项目模块
_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)

from loguru import logger

from services.session_service import get_session_service
from Agent.react_agent import get_react_agent


# ==================== 配置 ====================
TEST_USER_ID = 99999  # 测试用用户ID

# 测试用例
TEST_CASES = [
    {
        "name": "简单对话（不触发工具）",
        "query": "你好，你是谁？能帮我做什么？",
        "expect_tool": False,
    },
    {
        "name": "热量目标计算（触发 calorie_target_suggest）",
        "query": "我身高175cm，体重80kg，男性，久坐办公，帮我算一下每天该吃多少热量来减脂",
        "expect_tool": True,
    },
    {
        "name": "食物营养查询（触发 query_food_nutrition）",
        "query": "我想知道鸡蛋、牛奶、全麦面包这几种食物的热量和营养成分",
        "expect_tool": True,
    },
]


# ==================== 前置检查 ====================
def check_prerequisites() -> bool:
    """检查测试前置条件是否满足"""
    print("\n" + "=" * 60)
    print("前置条件检查")
    print("=" * 60)

    all_ok = True

    # 1. 检查 .env / 环境变量
    dashscope_key = os.environ.get("DASHSCOPE_API_KEY", "")
    if not dashscope_key:
        # 尝试从 .env 读取
        env_path = os.path.join(_PROJECT_ROOT, ".env")
        if os.path.exists(env_path):
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    if line.strip().startswith("DASHSCOPE_API_KEY"):
                        dashscope_key = line.split("=", 1)[1].strip()
                        break
    if dashscope_key:
        print(f"  [OK] DASHSCOPE_API_KEY: {dashscope_key[:8]}...")
    else:
        print("  [FAIL] DASHSCOPE_API_KEY 未配置")
        all_ok = False

    # 2. 检查 Redis 连接
    try:
        from services.redis_service import get_redis_service
        redis_svc = get_redis_service()
        redis_svc.client.ping()
        print("  [OK] Redis 连接正常")
    except Exception as e:
        print(f"  [FAIL] Redis 连接失败: {e}")
        all_ok = False

    # 3. 检查 Java 后端连通性
    try:
        import httpx
        resp = httpx.get("http://localhost:8088/api/chat/session/list?userId=1", timeout=5)
        if resp.status_code in (200, 401, 403):
            print(f"  [OK] Java 后端连通 (HTTP {resp.status_code})")
        else:
            print(f"  [WARN] Java 后端返回非预期状态码: {resp.status_code}")
    except Exception as e:
        print(f"  [FAIL] Java 后端连接失败: {e}")
        all_ok = False

    return all_ok


# ==================== SSE 事件消费 ====================
async def consume_stream(agent, session_id: str, query: str) -> dict:
    """
    消费 Agent 的 SSE 流式输出，打印事件并统计

    Returns:
        事件统计字典
    """
    stats = {
        "thought": [],
        "tool_call": [],
        "tool_result": [],
        "answer": "",
        "error": None,
        "events": 0,
    }

    async for sse_data in agent.execute_stream(session_id, query):
        # execute_stream 返回 "event: xxx\ndata: {...}\n\n" 格式
        lines = sse_data.strip().split("\n")
        event_type = ""
        event_data = ""

        for line in lines:
            if line.startswith("event: "):
                event_type = line[7:].strip()
            elif line.startswith("data: "):
                event_data = line[6:]

        stats["events"] += 1

        import json
        data = json.loads(event_data) if event_data else {}

        if event_type == "start":
            print(f"\n  [START] sessionId={data.get('sessionId')}")

        elif event_type == "thought":
            thought_text = data.get("content", "")
            stats["thought"].append(thought_text)
            preview = thought_text[:80] + "..." if len(thought_text) > 80 else thought_text
            print(f"  [THOUGHT] {preview}")

        elif event_type == "tool_call":
            tool_name = data.get("name")
            tool_args = data.get("args", {})
            stats["tool_call"].append({"name": tool_name, "args": tool_args})
            print(f"  [TOOL_CALL] {tool_name}({tool_args})")

        elif event_type == "tool_result":
            tool_name = data.get("name", "")
            result_text = data.get("content", "")
            stats["tool_result"].append({"name": tool_name, "content": result_text})
            preview = result_text[:100] + "..." if len(result_text) > 100 else result_text
            print(f"  [TOOL_RESULT] {tool_name}: {preview}")

        elif event_type == "answer":
            answer_text = data.get("content", "")
            stats["answer"] = answer_text
            print(f"  [ANSWER] {answer_text}")

        elif event_type == "error":
            stats["error"] = data.get("message", "未知错误")
            print(f"  [ERROR] {stats['error']}")

        elif event_type == "done":
            print(f"  [DONE] 共 {stats['events']} 个事件")

    return stats


# ==================== 测试用例 ====================
async def run_test_case(agent, session_service, case: dict, session_id: str) -> bool:
    """
    运行单个测试用例

    Returns:
        True=通过, False=失败
    """
    print(f"\n{'─' * 60}")
    print(f"测试用例: {case['name']}")
    print(f"用户提问: {case['query']}")
    print(f"{'─' * 60}")

    try:
        stats = await consume_stream(agent, session_id, case["query"])

        # 验证结果
        passed = True
        reasons = []

        # 1. 必须有最终回答
        if not stats["answer"]:
            passed = False
            reasons.append("未收到最终回答")

        # 2. 不能有错误
        if stats["error"]:
            passed = False
            reasons.append(f"收到错误事件: {stats['error']}")

        # 3. 工具调用预期检查
        if case["expect_tool"] and not stats["tool_call"]:
            passed = False
            reasons.append("预期触发工具调用但未触发")

        if not case["expect_tool"] and stats["tool_call"]:
            # 非强制失败，仅提示
            reasons.append(f"（提示）预期不触发工具但触发了: {[t['name'] for t in stats['tool_call']]}")

        # 结果输出
        if passed:
            print(f"\n  >>> 用例通过")
            if reasons:
                for r in reasons:
                    print(f"      {r}")
        else:
            print(f"\n  >>> 用例失败:")
            for r in reasons:
                print(f"      {r}")

        # 工具调用详情
        if stats["tool_call"]:
            print(f"\n  工具调用统计:")
            for tc in stats["tool_call"]:
                print(f"    - {tc['name']}({tc['args']})")

        return passed

    except Exception as e:
        print(f"\n  >>> 用例异常: {e}")
        logger.exception("测试用例执行异常")
        return False


# ==================== 主测试流程 ====================
async def main():
    print("\n" + "=" * 60)
    print("ReAct Agent 全链路集成测试")
    print("=" * 60)

    # 前置检查
    if not check_prerequisites():
        print("\n前置条件不满足，请检查后重试。")
        return

    # 初始化依赖
    print("\n" + "=" * 60)
    print("初始化 Agent 依赖")
    print("=" * 60)

    try:
        session_service = get_session_service()
        agent = get_react_agent()
        print("  [OK] SessionService 初始化完成")
        print("  [OK] ReActAgent 初始化完成")
        print(f"  [OK] 已注册工具: {list(agent.tools.keys())}")
    except Exception as e:
        print(f"  [FAIL] 依赖初始化失败: {e}")
        logger.exception("依赖初始化异常")
        return

    # 创建测试会话
    print("\n" + "=" * 60)
    print("创建测试会话")
    print("=" * 60)

    try:
        session_id = session_service.create_session(TEST_USER_ID)
        print(f"  [OK] 会话创建成功: session_id={session_id}")
    except Exception as e:
        print(f"  [FAIL] 会话创建失败: {e}")
        logger.exception("会话创建异常")
        return

    # 逐个运行测试用例
    results = []
    for case in TEST_CASES:
        passed = await run_test_case(agent, session_service, case, session_id)
        results.append({"name": case["name"], "passed": passed})

        # 用例间短暂等待
        await asyncio.sleep(1)

    # 验证会话历史
    print(f"\n{'=' * 60}")
    print("验证会话历史持久化")
    print("=" * 60)

    try:
        history = session_service.get_recent_history(session_id)
        print(f"  会话历史消息数: {len(history)}")
        for i, msg in enumerate(history):
            role = msg.get("role", "?")
            content = msg.get("content", "")
            preview = content[:60] + "..." if len(content) > 60 else content
            print(f"    [{i}] role={role}, content={preview}")
    except Exception as e:
        print(f"  [WARN] 会话历史读取失败: {e}")

    # 清理：落盘并清除缓存
    print(f"\n{'=' * 60}")
    print("清理测试数据")
    print("=" * 60)

    try:
        session_service.clear_session_cache(session_id)
        print(f"  [OK] 会话缓存已落盘并清除: {session_id}")
    except Exception as e:
        print(f"  [WARN] 清理失败: {e}")

    # 测试总结
    print(f"\n{'=' * 60}")
    print("测试总结")
    print("=" * 60)

    total = len(results)
    passed_count = sum(1 for r in results if r["passed"])

    for r in results:
        status = "PASS" if r["passed"] else "FAIL"
        print(f"  [{status}] {r['name']}")

    print(f"\n  通过: {passed_count}/{total}")

    if passed_count == total:
        print("\n  >>> 全部测试通过！Agent 全链路运行正常。")
    else:
        print("\n  >>> 部分测试未通过，请检查上方日志。")


if __name__ == "__main__":
    asyncio.run(main())
