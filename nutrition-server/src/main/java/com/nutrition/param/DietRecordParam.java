package com.nutrition.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "添加饮食记录请求参数")
public class DietRecordParam {

    @NotBlank(message = "记录日期不能为空")
    @Schema(description = "记录日期，格式：YYYY-MM-DD")
    private String recordDate;

    @NotBlank(message = "餐次类型不能为空")
    @Schema(description = "餐段类型：breakfast/lunch/dinner/snack")
    private String mealType;

    @NotEmpty(message = "饮食项列表不能为空")
    @Schema(description = "饮食项列表")
    private List<DietItemParam> items;

    @Schema(description = "整体备注")
    private String remark;

    @Data
    @Schema(description = "饮食项参数")
    public static class DietItemParam {

        @NotBlank(message = "食物名称不能为空")
        @Schema(description = "食物名称")
        private String foodName;

        @Schema(description = "食物描述，AI计算热量依据")
        private String foodDesc;

        @NotNull(message = "重量不能为空")
        @Schema(description = "食用重量(g)")
        private BigDecimal weight;

        @NotNull(message = "热量不能为空")
        @Schema(description = "总热量kcal（AI估算/手动填写）")
        private BigDecimal calories;

        @Schema(description = "单条食物备注")
        private String remark;

        @Schema(description = "食物图片附件ID列表，多个用逗号分隔")
        private String fileIds;
    }
}