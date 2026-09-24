package com.nutrition.service;

import com.nutrition.vo.CaptchaVO;

/**
 * 图形验证码服务
 * 提供验证码生成与一次性校验能力。
 */
public interface CaptchaService {

    /**
     * 创建新的四位数字图形验证码。
     *
     * @return 验证码标识和 Base64 图片
     */
    CaptchaVO createCaptcha();

    /**
     * 校验并消费验证码。
     * 无论校验成功还是失败，验证码都会被删除，避免同一验证码被反复尝试。
     *
     * @param captchaId 验证码唯一标识
     * @param code      用户输入的验证码
     */
    void validateCaptcha(String captchaId, String code);
}
