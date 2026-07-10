package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 月度打卡VO
 * 用于返回用户指定月份的打卡日期列表
 */
@Data
@Schema(description = "月度打卡日期VO")
public class CheckinMonthlyVO {

    @Schema(description = "打卡日期列表，格式：YYYY-MM-DD")
    private List<String> dates;
}
