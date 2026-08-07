"""文档处理服务 - 文档解析与切片"""
import os
import tempfile
from typing import List, Tuple
from loguru import logger
from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter
from constants.global_constants import FileConstants

# 支持的文件格式（从全局常量导入）
SUPPORTED_FORMATS = FileConstants.SUPPORTED_FORMATS


class DocumentService:
    """文档处理服务"""

    def __init__(self) -> None:
        pass

    def validate_file_format(self, filename: str) -> bool:
        """
        校验文件格式是否支持

        Args:
            filename: 文件名

        Returns:
            是否支持
        """
        _, ext = os.path.splitext(filename.lower())
        return ext in SUPPORTED_FORMATS

    def parse_document(self, file_content: bytes, filename: str) -> str:
        """
        解析文档内容，提取纯文本

        Args:
            file_content: 文件二进制内容
            filename: 文件名（用于判断格式）

        Returns:
            提取的纯文本内容
        """
        ext = os.path.splitext(filename.lower())[1]
        logger.info("开始解析文档: filename={}, ext={}", filename, ext)

        # 保存到临时文件
        tmp_dir = tempfile.mkdtemp()
        tmp_path = os.path.join(tmp_dir, filename)

        try:
            with open(tmp_path, "wb") as f:
                f.write(file_content)

            # 根据格式选择解析方式
            if ext == ".pdf":
                text = self._parse_pdf(tmp_path)
            elif ext in (".txt", ".md"):
                text = self._parse_text(tmp_path)
            elif ext == ".docx":
                text = self._parse_docx(tmp_path)
            elif ext == ".doc":
                # .doc格式需要特殊处理（暂不支持，提示转换）
                raise ValueError("暂不支持.doc格式，请转换为.docx或.pdf后重试")
            elif ext == ".json":
                text = self._parse_json(file_content)
            else:
                raise ValueError(f"不支持的文件格式: {ext}")

            if not text or not text.strip():
                raise ValueError("文档内容为空")

            logger.info("文档解析成功: filename={}, text_length={}", filename, len(text))
            return text

        finally:
            # 清理临时文件
            try:
                if os.path.exists(tmp_path):
                    os.remove(tmp_path)
                if os.path.exists(tmp_dir):
                    os.rmdir(tmp_dir)
            except Exception:
                pass

    def _parse_pdf(self, file_path: str) -> str:
        """解析PDF文件"""
        from PyPDF2 import PdfReader

        reader = PdfReader(file_path)
        text = ""
        for page in reader.pages:
            page_text = page.extract_text()
            if page_text:
                text += page_text + "\n"

        return text

    def _parse_text(self, file_path: str) -> str:
        """解析文本文件（txt/md）"""
        with open(file_path, "r", encoding="utf-8") as f:
            return f.read()

    def _parse_docx(self, file_path: str) -> str:
        """解析Word文档"""
        from docx import Document as DocxDocument

        doc = DocxDocument(file_path)
        text = ""
        for paragraph in doc.paragraphs:
            if paragraph.text.strip():
                text += paragraph.text + "\n"

        # 读取表格内容
        for table in doc.tables:
            for row in table.rows:
                for cell in row.cells:
                    if cell.text.strip():
                        text += cell.text + "\n"

        return text

    def _parse_json(self, file_content: bytes) -> str:
        """
        解析JSON结构化文件，将JSON数据转换为通顺的自然语言文本
        禁止直接将原始json字符串送入文本分割器，防止字段结构被切片拆分、破坏语义

        支持两种格式：
        1. 根节点为数组：循环遍历每一条结构体，拼接成通顺易懂的自然语言文本
        2. 根节点为单个json对象：格式化转为可读文本
        """
        import json

        # UTF-8解码，处理BOM头
        try:
            text_raw = file_content.decode("utf-8-sig")
        except UnicodeDecodeError:
            try:
                text_raw = file_content.decode("gbk")
            except UnicodeDecodeError:
                raise ValueError("JSON文件编码无法识别，请使用UTF-8编码")

        try:
            data = json.loads(text_raw)
        except json.JSONDecodeError as e:
            raise ValueError(f"JSON格式解析失败: {str(e)}")

        # 将JSON数据转换为自然语言文本
        if isinstance(data, list):
            # 根节点为数组：遍历每一条结构体
            text_parts = []
            for index, item in enumerate(data, 1):
                if isinstance(item, dict):
                    text_parts.append(self._json_object_to_text(item, index))
                else:
                    text_parts.append(f"第{index}条记录：{str(item)}")
            return "\n\n".join(text_parts)

        elif isinstance(data, dict):
            # 根节点为单个json对象
            return self._json_object_to_text(data)

        else:
            # 简单类型（字符串/数字等）
            return str(data)

    def _json_object_to_text(self, obj: dict, index: int = None) -> str:
        """
        将单个JSON对象转换为通顺可读的自然语言文本

        Args:
            obj: JSON字典
            index: 记录序号（可选）

        Returns:
            自然语言文本
        """
        prefix = f"第{index}条记录：" if index else ""
        parts = []
        for key, value in obj.items():
            # 将键名中的下划线、驼峰转为可读中文描述
            readable_key = key.replace("_", " ").replace("-", " ")
            if isinstance(value, (dict, list)):
                # 嵌套结构递归处理
                import json
                value_str = json.dumps(value, ensure_ascii=False)
            else:
                value_str = str(value)
            parts.append(f"{readable_key}：{value_str}")

        return prefix + "；".join(parts) + "。"

    def split_document(self, text: str, filename: str) -> List[Document]:
        """
        将文档文本切片

        Args:
            text: 文档纯文本
            filename: 文件名（用于元数据）

        Returns:
            切片后的文档列表
        """
        from config.settings import settings

        logger.info("开始文本切片: filename={}, chunk_size={}, chunk_overlap={}",
                    filename, settings.chunk_size, settings.chunk_overlap)

        # 初始化分割器
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=settings.chunk_size,
            chunk_overlap=settings.chunk_overlap,
            separators=settings.separators,
            length_function=len
        )

        # 创建文档对象
        document = Document(
            page_content=text,
            metadata={
                "filename": filename,
                "chunk_size": settings.chunk_size
            }
        )

        # 执行切片
        chunks = splitter.split_documents([document])

        logger.info("文本切片完成: filename={}, chunks={}", filename, len(chunks))
        return chunks

    def process_document(self, file_content: bytes, filename: str) -> Tuple[List[Document], int]:
        """
        完整处理流程：解析 -> 切片

        Args:
            file_content: 文件二进制内容
            filename: 文件名

        Returns:
            (切片文档列表, 切片数量)
        """
        # 校验格式
        if not self.validate_file_format(filename):
            supported = ", ".join(SUPPORTED_FORMATS)
            raise ValueError(f"不支持的文件格式，支持的格式: {supported}")

        # 解析文档
        text = self.parse_document(file_content, filename)

        # 切片
        chunks = self.split_document(text, filename)

        return chunks, len(chunks)
