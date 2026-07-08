package com.nutrition.util;

/**
 * ID转换工具类
 * 提供Long与String之间的安全转换
 */
public class IdConvertUtil {

    /**
     * 安全字符串转Long雪花ID
     *
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

    /**
     * 安全Long转String
     *
     * @param id Long类型ID
     * @return 转换后的字符串，null返回空字符串
     */
    public static String toString(Long id) {
        if (id == null) {
            return "";
        }
        return String.valueOf(id);
    }
}
