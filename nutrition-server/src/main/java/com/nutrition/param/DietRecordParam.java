package com.nutrition.param;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

/**
 * 饮食记录参数类
 * 用于接收饮食记录添加的请求参数
 */
@Data
public class DietRecordParam {

    @NotBlank(message = "日期不能为空")
    private String recordDate;

    @NotBlank(message = "餐次类型不能为空")
    private String mealType;

    @NotEmpty(message = "食物明细不能为空")
    private List<DietItemParam> items;

    private String remark;

    @Data
    public static class DietItemParam {
        @NotNull(message = "食物ID不能为空")
        private Long foodId;

        @NotBlank(message = "食物名称不能为空")
        private String foodName;

        @NotNull(message = "重量不能为空")
        @Min(value = 1, message = "重量必须大于0")
        @Max(value = 10000, message = "单次重量不能超过10000g")
        private Integer weight;
    }
}
