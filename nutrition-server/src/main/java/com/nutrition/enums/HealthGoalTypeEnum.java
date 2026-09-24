package com.nutrition.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 健康分析目标类型枚举
 * 统一管理用户目标编码、中文名称和 AI 分析侧重点。
 */
@Getter
@RequiredArgsConstructor
public enum HealthGoalTypeEnum {

    /** 健身目标 */
    FITNESS("fitness", "健身", "重点关注蛋白质摄入、训练恢复和能量供应，避免极端热量缺口。"),

    /** 减肥目标 */
    WEIGHT_LOSS("weight_loss", "减肥", "重点关注温和热量缺口、饱腹感、蛋白质和膳食纤维摄入。"),

    /** 正常饮食目标 */
    NORMAL_DIET("normal_diet", "正常饮食", "重点关注营养均衡、规律进餐和每日摄入稳定性。"),

    /** 增重目标 */
    WEIGHT_GAIN("weight_gain", "增重", "重点关注适量热量盈余、营养密度、多餐安排和力量训练恢复。");

    /** 对外传输编码 */
    private final String code;

    /** 中文名称 */
    private final String label;

    /** AI 分析侧重点 */
    private final String promptDirection;

    /**
     * 根据编码转换为目标枚举。
     *
     * @param code 目标编码
     * @return 目标枚举；未知编码返回 null，由参数校验统一处理
     */
    @JsonCreator
    public static HealthGoalTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HealthGoalTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 输出前端使用的目标编码。
     *
     * @return 目标编码
     */
    @JsonValue
    public String getCode() {
        return code;
    }
}
