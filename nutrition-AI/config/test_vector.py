"""
DashVector 连接验证脚本
判定标准：返回对象的 code == 0 才代表成功；任何非0（-2980/4/401/404等）都是失败

使用方法：
  1. 确保 .env 中 VECTOR_API_KEY / VECTOR_ENDPOINT / VECTOR_COLLECTION_NAME 已正确配置
  2. 运行: python config/test_vector.py
"""
import sys
import os

# 将项目根目录加入 sys.path，以便导入 config.settings
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config.settings import settings
from constants.global_constants import VectorConstants
import dashvector
from dashvector import Doc

COLLECTION_NAME = settings.vector_collection_name


def check(resp, step: str) -> bool:
    """通用结果检查：code==0 成功，否则失败"""
    code = getattr(resp, "code", 0)
    if code in (0, None, ""):
        print(f"[OK] {step}")
        return True
    msg = getattr(resp, "message", "N/A")
    req_id = getattr(resp, "request_id", getattr(resp, "requests_id", "N/A"))
    print(f"[FAIL] {step} -> code={code}, message={msg}, request_id={req_id}")
    return False


print("=" * 60)
print("DashVector 连接验证开始")
print(f"ENDPOINT: {settings.vector_endpoint}")
print(f"COLLECTION: {COLLECTION_NAME}")
print("=" * 60)

# ==================== 步骤1: 创建客户端 ====================
print("\n>>> 步骤1: 创建客户端")
try:
    client = dashvector.Client(api_key=settings.vector_api_key, endpoint=settings.vector_endpoint)
    print("[OK] 客户端创建成功（仅本地对象，不代表鉴权通过）")
except Exception as e:
    print(f"[FAIL] 客户端创建异常: {e}")
    raise SystemExit(1)

# ==================== 步骤2: 列出所有集合（控制面，验证鉴权） ====================
print("\n>>> 步骤2: 列出所有集合 (client.list)")
try:
    resp = client.list()
    if check(resp, "列出集合"):
        output = getattr(resp, "output", None)
        print(f"    集合列表: {output}")
    else:
        print("    （继续尝试步骤3，确认是否集合级问题）")
except Exception as e:
    print(f"[FAIL] client.list 异常: {e}")

# ==================== 步骤3: 获取指定集合（验证集合是否存在） ====================
print("\n>>> 步骤3: 获取集合 (client.get)")
coll = None
try:
    coll = client.get(COLLECTION_NAME)
    code = getattr(coll, "code", 0)
    if code in (0, None, ""):
        print(f"[OK] 获取集合成功: {COLLECTION_NAME}")
        for attr in ("name", "dimension", "metric", "partition_count", "status"):
            val = getattr(coll, attr, None)
            if val is not None:
                print(f"    {attr} = {val}")
    else:
        msg = getattr(coll, "message", "N/A")
        print(f"[FAIL] 获取集合 -> code={code}, message={msg}")
except Exception as e:
    print(f"[FAIL] client.get 异常: {e}")

if coll is None:
    raise SystemExit(1)

# ==================== 步骤4: 插入一条测试数据（数据面，验证完整链路） ====================
print("\n>>> 步骤4: 插入测试数据 (collection.insert)")
dim = VectorConstants.VECTOR_DIMENSION
print(f"    使用向量维度: {dim}")
try:
    ret = coll.insert(
        Doc(
            id="conn_test_001",
            vector=[0.01] * int(dim),
            fields={"text": "连接测试数据", "doc_id": "conn_test", "file_md5": "conn_test_md5"}
        )
    )
    if check(ret, "插入测试数据"):
        print(f"    插入成功, request_id={getattr(ret, 'request_id', 'N/A')}")
    else:
        raise SystemExit(1)
except Exception as e:
    print(f"[FAIL] collection.insert 异常: {e}")
    raise SystemExit(1)

# ==================== 步骤5: 查询测试数据（验证可检索） ====================
print("\n>>> 步骤5: 查询测试数据 (collection.query)")
try:
    qret = coll.query(
        vector=[0.01] * int(dim),
        topk=1,
        filter="doc_id = 'conn_test'",  # SDK 1.0.x 只支持字符串表达式，不支持dict
        output_fields=["text", "doc_id"],
        include_vector=False,
    )
    if check(qret, "查询测试数据"):
        output = getattr(qret, "output", [])
        print(f"    查询结果: {output}")
except Exception as e:
    print(f"[FAIL] collection.query 异常: {e}")

# ==================== 步骤6: 删除测试数据（清理） ====================
print("\n>>> 步骤6: 删除测试数据 (collection.delete)")
try:
    dret = coll.delete(["conn_test_001"])
    if check(dret, "删除测试数据"):
        print("    清理完成")
except Exception as e:
    print(f"[FAIL] collection.delete 异常: {e}")

print("\n" + "=" * 60)
print("验证结束：所有步骤均为 [OK] 即代表连接正常")
print("=" * 60)
