package com.nutrition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应对象
 * 前端通过 captchaId 提交注册请求，图片仅用于用户识别数字，不包含验证码明文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图形验证码")
public class CaptchaVO {

    /** 验证码唯一标识 */
    @Schema(description = "验证码唯一标识")
    private String captchaId;

    /** Base64 编码的 PNG 图片 */
    @Schema(description = "Base64 编码的 PNG 图片")
    private String imageBase64;
}
