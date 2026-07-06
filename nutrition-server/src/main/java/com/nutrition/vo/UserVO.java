package com.nutrition.vo;

import lombok.Data;

/**
 * 用户信息视图对象
 * 用于返回用户个人信息及营养目标
 */
@Data
public class UserVO {

    private Long id;

    private String openid;

    private String nickname;

    private String fileIds;

    private String email;

    private Integer dailyCalorieGoal;

    private Integer dailyProteinGoal;

    private Integer dailyFatGoal;

    private Integer dailyCarbsGoal;
}
