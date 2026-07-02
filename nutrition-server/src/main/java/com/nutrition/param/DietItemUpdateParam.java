package com.nutrition.param;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 饮食明细更新参数类
 * 用于接收饮食明细重量更新的请求参数
 */
@Data
public class DietItemUpdateParam {

    @NotNull(message = "重量不能为空")
    @Min(value = 1, message = "重量必须大于0")
    private Integer weight;
}
