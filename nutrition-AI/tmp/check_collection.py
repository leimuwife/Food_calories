import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
# 覆盖进程内继承到的旧 Endpoint，使用用户提供的新 Endpoint
os.environ["VECTOR_ENDPOINT"] = "vrs-cn-eiz4z0fss0001q.dashvector.cn-beijing.aliyuncs.com"

import dashvector
from config.settings import settings

print("settings.vector_endpoint  =", settings.vector_endpoint)
print("settings.vector_collection=", settings.vector_collection_name)
k = settings.vector_api_key
print("api_key(prefix)           =", (k[:8] + "..." + k[-6:]) if k else "<EMPTY>")
print("-" * 60)

client = dashvector.Client(api_key=settings.vector_api_key, endpoint=settings.vector_endpoint)

lst = client.list()
print("client.list() code =", getattr(lst, "code", 0), "output =", getattr(lst, "output", None))

coll = client.get(settings.vector_collection_name)
print("client.get(name=%s) code = %s message = %s" %
      (settings.vector_collection_name, getattr(coll, "code", 0), getattr(coll, "message", "")))

meta = getattr(coll, "_collection_meta", None)
if meta is not None:
    for attr in sorted(dir(meta)):
        if attr.startswith("_"):
            continue
        try:
            val = getattr(meta, attr)
        except Exception:
            continue
        if callable(val):
            continue
        print("   meta.%s = %r" % (attr, val))

try:
    st = coll.stats()
    print("collection.stats() code =", getattr(st, "code", 0),
          "dict =", (st.to_dict() if hasattr(st, "to_dict") else st))
except Exception as e:
    print("stats error:", e)
