"""
DashScope 连接验证脚本 - 验证 text-embedding-v4 可用性

使用方法：
  1. 确保 .env 中 DASHSCOPE_API_KEY 已正确配置
  2. 运行: python config/test_dashscope.py
"""
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config.settings import settings
from constants.global_constants import VectorConstants
import dashscope
from dashscope import TextEmbedding

dashscope.api_key = settings.dashscope_api_key

print("=" * 60)
print("DashScope Embedding 验证（API Key 鉴权）")
print("=" * 60)

try:
    resp = TextEmbedding.call(
        model=settings.embedding_model,
        input="这是一个测试文本，用于验证DashScope连接",
        dimension=VectorConstants.VECTOR_DIMENSION
    )
    print(f"HTTP状态码: {resp.status_code}")
    if resp.status_code == 200:
        output = resp.output
        embedding = output.get("embeddings", [])[0].get("embedding", [])
        print(f"[OK] 调用成功! embedding维度: {len(embedding)}")
        print(f"[OK] usage: {output.get('usage', {})}")
    else:
        print(f"[FAIL] code: {getattr(resp, 'code', 'N/A')}")
        print(f"[FAIL] message: {getattr(resp, 'message', 'N/A')}")
        if hasattr(resp, 'request_id'):
            print(f"[FAIL] request_id: {resp.request_id}")
except Exception as e:
    print(f"[FAIL] 调用异常: {e}")
