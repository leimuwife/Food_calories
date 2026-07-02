package com.nutrition.param;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户注册参数类
 * 用于接收用户注册的请求参数
 */
@Data
public class RegisterParam {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度为3-32位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长32位")
    private String nickname;
}
