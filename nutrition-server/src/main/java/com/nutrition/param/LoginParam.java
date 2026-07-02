package com.nutrition.param;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户登录参数类
 * 用于接收用户登录的请求参数
 */
@Data
public class LoginParam {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;
}
