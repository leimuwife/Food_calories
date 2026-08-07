"""
端到端完整链路验证：真实 Embedding(1024维) → 向量库插入 → 查询 → 删除
完全模拟生产 insert_documents 的逻辑

使用方法：
  1. 确保 .env 中所有配置已正确填写
  2. 运行: python config/test_e2e.py
"""
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config.settings import settings
from constants.global_constants import VectorConstants
import dashvector
from dashvector import Doc
from langchain_community.embeddings.dashscope import DashScopeEmbeddings

# ==================== 初始化 ====================
print("=== 1. 初始化 Embedding 和 DashVector 客户端 ===")
embeddings = DashScopeEmbeddings(model=settings.embedding_model, dashscope_api_key=settings.dashscope_api_key)
client = dashvector.Client(api_key=settings.vector_api_key, endpoint=settings.vector_endpoint)
collection = client.get(settings.vector_collection_name)
code = getattr(collection, "code", 0)
if code not in (0, None, ""):
    print(f"[FAIL] 获取集合: code={code}, message={getattr(collection, 'message', 'N/A')}")
    raise SystemExit(1)
print(f"[OK] 初始化完成, collection = {settings.vector_collection_name}")

# ==================== 2. 真实 Embedding ====================
print("\n=== 2. 调用 DashScope 生成真实向量 ===")
texts = ["这是第一段知识库文本", "这是第二段知识库文本", "这是第三段知识库文本"]
vectors = []
for t in texts:
    vec = embeddings.embed_query(t)
    vectors.append(vec)
    print(f"    文本: {t[:20]}... -> 向量维度: {len(vec)}")
if len(vectors[0]) != VectorConstants.VECTOR_DIMENSION:
    print(f"[FAIL] 向量维度 {len(vectors[0])} != {VectorConstants.VECTOR_DIMENSION}, 与集合维度不匹配")
    raise SystemExit(1)
print(f"[OK] {len(texts)}条文本全部生成向量, 维度={VectorConstants.VECTOR_DIMENSION}")

# ==================== 3. 插入向量库 ====================
print("\n=== 3. 插入向量库 ===")
docs = []
for i, (t, vec) in enumerate(zip(texts, vectors)):
    docs.append(
        Doc(
            id=f"e2e_test_chunk_{i}",
            vector=vec,
            fields={"text": t, "doc_id": "e2e_test", "file_md5": "e2e_test_md5", "chunk_index": str(i)}
        )
    )
ret = collection.insert(docs)
r_code = getattr(ret, "code", 0)
if r_code not in (0, None, ""):
    print(f"[FAIL] 插入: code={r_code}, message={getattr(ret, 'message', 'N/A')}")
    raise SystemExit(1)
print(f"[OK] 插入成功, request_id={getattr(ret, 'request_id', 'N/A')}")

# ==================== 4. 查询向量库 ====================
print("\n=== 4. 查询向量库（用第一条文本向量检索）===")
qret = collection.query(
    vector=vectors[0],
    topk=3,
    filter="doc_id = 'e2e_test'",
    output_fields=["text", "doc_id", "chunk_index"],
    include_vector=False,
)
q_code = getattr(qret, "code", 0)
if q_code not in (0, None, ""):
    print(f"[FAIL] 查询: code={q_code}, message={getattr(qret, 'message', 'N/A')}")
    raise SystemExit(1)
output = getattr(qret, "output", [])
print(f"[OK] 查询成功, 返回 {len(output)} 条结果:")
for item in output:
    print(f"    id={item.id}, score={item.score:.4f}, text={item.fields.get('text', '')[:20]}")

# ==================== 5. 清理测试数据 ====================
print("\n=== 5. 清理测试数据 ===")
ids = [f"e2e_test_chunk_{i}" for i in range(len(texts))]
dret = collection.delete(ids)
d_code = getattr(dret, "code", 0)
if d_code not in (0, None, ""):
    print(f"[FAIL] 删除: code={d_code}, message={getattr(dret, 'message', 'N/A')}")
    raise SystemExit(1)
print(f"[OK] 删除成功, request_id={getattr(dret, 'request_id', 'N/A')}")

print("\n" + "=" * 60)
print("端到端验证完成: Embedding -> 插入 -> 查询 -> 删除 全部通过")
print("=" * 60)
