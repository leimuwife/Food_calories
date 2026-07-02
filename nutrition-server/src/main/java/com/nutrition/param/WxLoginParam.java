package com.nutrition.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录参数类
 * 用于接收微信小程序登录的请求参数
 */
@Data
public class WxLoginParam {

    @NotBlank(message = "code不能为空")
    private String code;
}
