package com.nutrition.enums;

import lombok.Getter;

/**
 * RAG知识库文档状态枚举
 * 1正常 2向量入库中 3入库失败 4已删除
 */
@Getter
public enum RagDocumentStatusEnum {

    NORMAL(1, "正常"),
    PROCESSING(2, "向量入库中"),
    FAILED(3, "入库失败"),
    DELETED(4, "已删除");

    private final int code;
    private final String desc;

    RagDocumentStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return 枚举值，未匹配返回null
     */
    public static RagDocumentStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RagDocumentStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据状态码获取描述
     * @param code 状态码
     * @return 描述
     */
    public static String getDescByCode(Integer code) {
        RagDocumentStatusEnum status = fromCode(code);
        return status != null ? status.getDesc() : "未知";
    }
}
