package com.nutrition.param;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户注册参数类
 * 用于接收用户名、密码、确认密码和图形验证码等注册请求参数。
 */
@Data
public class RegisterParam {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度为3-32位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{4}", message = "验证码必须是4位数字")
    private String captchaCode;

    /** 昵称，可为空；为空时使用用户名 */
    @Size(max = 32, message = "昵称最长32位")
    private String nickname;
}
