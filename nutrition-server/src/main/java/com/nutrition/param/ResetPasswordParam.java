package com.nutrition.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码参数类。
 * <p>
 * 用户忘记密码时，通过「用户名 + 注册手机号」校验身份后设置新密码。
 * 手机号不唯一，仅作为身份校验因子，必须与用户名对应的账号手机号一致。
 * </p>
 */
@Data
public class ResetPasswordParam {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入合法的11位手机号")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
