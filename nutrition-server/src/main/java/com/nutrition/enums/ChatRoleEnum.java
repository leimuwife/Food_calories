package com.nutrition.enums;

import lombok.Getter;

/**
 * AI聊天消息角色枚举
 * Python端role字符串与数据库role编码(Long)的映射
 * Python合法取值：user / ai_thought / tool_call / tool_result / ai_answer
 */
@Getter
public enum ChatRoleEnum {

    /** 用户消息 */
    USER(1L, "user"),
    /** AI思考过程（ReAct中间推理，不展示给用户） */
    AI_THOUGHT(2L, "ai_thought"),
    /** 工具调用（ReAct Agent发起的工具请求） */
    TOOL_CALL(3L, "tool_call"),
    /** 工具调用结果 */
    TOOL_RESULT(4L, "tool_result"),
    /** AI最终回答 */
    AI_ANSWER(5L, "ai_answer");

    /** 数据库编码 */
    private final Long code;
    /** 字符串标识 */
    private final String role;

    ChatRoleEnum(Long code, String role) {
        this.code = code;
        this.role = role;
    }

    /**
     * 根据角色字符串获取数据库编码
     *
     * @param role 角色字符串（user/ai_thought/tool_call/tool_result/ai_answer）
     * @return 编码；非法角色返回null
     */
    public static Long getCodeByRole(String role) {
        if (role == null) {
            return null;
        }
        for (ChatRoleEnum item : values()) {
            if (item.role.equals(role)) {
                return item.code;
            }
        }
        return null;
    }

    /**
     * 根据数据库编码获取角色字符串
     *
     * @param code 数据库编码
     * @return 角色字符串；未知编码返回null
     */
    public static String getRoleByCode(Long code) {
        if (code == null) {
            return null;
        }
        for (ChatRoleEnum item : values()) {
            if (item.code.equals(code)) {
                return item.role;
            }
        }
        return null;
    }
}
