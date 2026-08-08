import hashlib
'''
获取文件的MD5值
'''
def get_file_md5(file_path: str) -> str:
    md5_hash = hashlib.md5()
    with open(file_path,"rb") as f:
        for chunk in iter(lambda: f.read(65536),b''):
            md5_hash.update(chunk)
    return md5_hash.hexdigest()

print("当前清洗后文件MD5：",get_file_md5(r"D:\桌面\food_knowledge_clean.jsonl"))