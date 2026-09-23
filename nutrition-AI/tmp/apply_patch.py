# -*- coding: utf-8 -*-
"""一次性补丁脚本：落地 DashVector 集合 Schema 相关的 4 处改动"""
import io, os

ROOT = r"D:\JAVA\project\nutrition-all\nutrition-AI"


def patch(rel, old_lines, new_lines, expect=1):
    p = os.path.join(ROOT, rel)
    with io.open(p, "r", encoding="utf-8", newline="") as f:
        s = f.read()
    nl = "\r\n" if "\r\n" in s else "\n"
    old = nl.join(old_lines)
    new = nl.join(new_lines)
    n = s.count(old)
    if n != expect:
        raise SystemExit("FAIL %s: expected %d occurrence(s), found %d" % (rel, expect, n))
    s = s.replace(old, new)
    with io.open(p, "w", encoding="utf-8", newline="") as f:
        f.write(s)
    print("OK  %s  (newline=%s)" % (rel, "CRLF" if nl == "\r\n" else "LF"))


# ---- [3] config/settings.py 默认集合名 ----
patch(
    r"config\settings.py",
    ['    vector_collection_name: str = "food_knowledge_base"'],
    ['    vector_collection_name: str = "food_nutrition_knowledge"'],
)

# ---- [1] constants/global_constants.py 新增 FIELDS_SCHEMA / CREATE_TIMEOUT ----
patch(
    r"constants\global_constants.py",
    [
        "    # 当前项目使用的向量维度（必须与DashVector集合维度、Embedding输出维度一致）",
        "    VECTOR_DIMENSION = 1024",
    ],
    [
        "    # 当前项目使用的向量维度（必须与DashVector集合维度、Embedding输出维度一致）",
        "    VECTOR_DIMENSION = 1024",
        "",
        "    # 创建集合时声明的字段Schema（DashVector规定：集合创建后Schema不可更改）",
        "    # 只有在此声明的字段才能用于 filter 过滤查询（doc_id / file_md5 为过滤字段）",
        "    # 注意：chunk_index 必须是 str —— services/vector_service.py 写入的是 str(chunk_index)",
        "    FIELDS_SCHEMA = {",
        '        "text": str,',
        '        "doc_id": str,',
        '        "file_md5": str,',
        '        "chunk_index": str,',
        '        "filename": str,',
        "    }",
        "",
        "    # 创建集合后等待 ready 的超时时间（秒），避免 create 后立刻 get 拿到未就绪的假失败",
        "    CREATE_TIMEOUT = 60",
    ],
)

# ---- [2] services/vector_service.py 的 create() 补 fields_schema + timeout ----
patch(
    r"services\vector_service.py",
    [
        "        self.client.create(",
        "            name=settings.vector_collection_name,",
        "            dimension=VectorConstants.VECTOR_DIMENSION,",
        '            metric="cosine"',
        "        )",
    ],
    [
        "        self.client.create(",
        "            name=settings.vector_collection_name,",
        "            dimension=VectorConstants.VECTOR_DIMENSION,",
        '            metric="cosine",',
        "            fields_schema=VectorConstants.FIELDS_SCHEMA,",
        "            timeout=VectorConstants.CREATE_TIMEOUT",
        "        )",
    ],
)

# ---- [4] .env 恢复 3 行配置 ----
patch(
    r".env",
    [
        "# 向量库访问地址（Cluster Endpoint）",
        "#VECTOR_ENDPOINT",
        "# 向量库API密钥（在阿里云DashVector控制台获取）",
        "#VECTOR_API_KEY",
        "# 向量库集合名称",
        "#VECTOR_COLLECTION_NAME",
    ],
    [
        "# 向量库访问地址（Cluster Endpoint）",
        "VECTOR_ENDPOINT=vrs-cn-eiz4z0fss0001q.dashvector.cn-beijing.aliyuncs.com",
        "# 向量库API密钥（在阿里云DashVector控制台获取）",
        "# 注意：系统环境变量优先级高于本文件，已配置系统级 VECTOR_API_KEY 时以系统变量为准",
        "VECTOR_API_KEY=your-dashvector-api-key",
        "# 向量库集合名称",
        "VECTOR_COLLECTION_NAME=food_nutrition_knowledge",
    ],
)

print("ALL PATCHED")
