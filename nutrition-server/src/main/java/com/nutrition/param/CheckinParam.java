package com.nutrition.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 打卡请求参数
 * 使用 @Valid 注解进行参数校验
 */
@Data
@Schema(description = "打卡请求参数")
public class CheckinParam {

    @NotBlank(message = "打卡日期不能为空")
    @Schema(description = "打卡日期，格式：YYYY-MM-DD", example = "2026-07-07")
    private String date;
}
