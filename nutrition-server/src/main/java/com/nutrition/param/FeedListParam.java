package com.nutrition.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 获取动态列表请求参数
 */
@Data
@Schema(description = "获取动态列表请求参数")
public class FeedListParam {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于1")
    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Schema(description = "每页条数", example = "20")
    private Integer pageSize;
}