"""MD5计算工具"""
import hashlib
from loguru import logger


def calculate_md5(file_content: bytes) -> str:
    """
    计算文件二进制内容的MD5值

    Args:
        file_content: 文件二进制内容

    Returns:
        MD5哈希字符串（32位小写十六进制）
    """
    md5_hash = hashlib.md5()
    md5_hash.update(file_content)
    result = md5_hash.hexdigest()
    logger.debug("MD5计算完成: {}", result)
    return result


def verify_md5(file_content: bytes, expected_md5: str) -> bool:
    """
    校验文件内容的MD5是否与预期值一致

    Args:
        file_content: 文件二进制内容
        expected_md5: 预期的MD5值

    Returns:
        是否一致
    """
    actual_md5 = calculate_md5(file_content)
    is_match = actual_md5 == expected_md5

    if not is_match:
        logger.warning("MD5校验不一致: expected={}, actual={}", expected_md5, actual_md5)
    else:
        logger.debug("MD5校验通过: {}", expected_md5)

    return is_match
