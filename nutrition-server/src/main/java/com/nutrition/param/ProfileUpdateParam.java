package com.nutrition.param;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人信息更新参数类
 * 用于接收用户个人信息更新的请求参数
 */
@Data
public class ProfileUpdateParam {

    @Size(max = 32, message = "昵称最长32位")
    private String nickname;

    private String avatar;

    @Size(max = 128, message = "邮箱最长128位")
    private String email;
}
