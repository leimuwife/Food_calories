package com.nutrition.health;

import com.nutrition.enums.HealthGoalTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 健康目标枚举测试。
 */
class HealthGoalTypeEnumTest {

    /** 验证四种目标编码和中文名称可以正确转换。 */
    @Test
    void shouldConvertAllSupportedGoals() {
        assertEquals(HealthGoalTypeEnum.FITNESS, HealthGoalTypeEnum.fromCode("fitness"));
        assertEquals(HealthGoalTypeEnum.WEIGHT_LOSS, HealthGoalTypeEnum.fromCode("weight_loss"));
        assertEquals(HealthGoalTypeEnum.NORMAL_DIET, HealthGoalTypeEnum.fromCode("normal_diet"));
        assertEquals(HealthGoalTypeEnum.WEIGHT_GAIN, HealthGoalTypeEnum.fromCode("weight_gain"));
        assertEquals("健身", HealthGoalTypeEnum.FITNESS.getLabel());
    }

    /** 验证非法编码返回空，供请求校验处理。 */
    @Test
    void shouldReturnNullForUnknownGoal() {
        assertNull(HealthGoalTypeEnum.fromCode("unknown"));
    }
}
