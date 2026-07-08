package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 饮食项视图对象
 * 用于返回饮食项的详细信息
 */
@Data
public class DietItemVO {

    @Schema(description = "饮食项ID")
    private String id;

    @Schema(description = "记录ID")
    private String recordId;

    @Schema(description = "食物名称")
    private String foodName;

    @Schema(description = "食物描述")
    private String foodDesc;

    @Schema(description = "食用重量(g)")
    private BigDecimal weight;

    @Schema(description = "热量(kcal)")
    private BigDecimal calories;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "食物图片URL列表")
    private java.util.List<String> imageUrls;
}