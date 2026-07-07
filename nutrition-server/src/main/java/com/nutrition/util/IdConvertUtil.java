package com.nutrition.util;

public class IdConvertUtil {
    /**
     * 安全字符串转Long雪花ID
     * @param idStr 前端传入id字符串
     * @return 转换后的Long，空/非法返回null
     */
    public static Long toLong(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
