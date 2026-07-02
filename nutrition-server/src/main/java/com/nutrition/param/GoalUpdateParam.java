package com.nutrition.param;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 营养目标更新参数类
 * 用于接收用户营养目标更新的请求参数
 */
@Data
public class GoalUpdateParam {

    @Min(value = 0, message = "每日热量目标不能为负数")
    private Integer dailyCalorieGoal;

    @Min(value = 0, message = "每日蛋白质目标不能为负数")
    private Integer dailyProteinGoal;

    @Min(value = 0, message = "每日脂肪目标不能为负数")
    private Integer dailyFatGoal;

    @Min(value = 0, message = "每日碳水目标不能为负数")
    private Integer dailyCarbsGoal;
}
