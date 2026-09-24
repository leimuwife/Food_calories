package com.nutrition.service.impl;

import com.nutrition.common.BusinessException;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.CaptchaConfigEnum;
import com.nutrition.enums.CaptchaRedisKeyEnum;
import com.nutrition.service.CaptchaService;
import com.nutrition.util.CaptchaImageUtil;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现
 * 验证码仅以 Redis 中的明文数字为校验依据，前端只持有验证码标识和图片。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    /** 安全随机数生成器 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** Redis 缓存工具 */
    private final RedisCache redisCache;

    /**
     * 创建四位数字图形验证码并写入 Redis。
     *
     * @return 验证码标识和 Base64 图片
     */
    @Override
    public CaptchaVO createCaptcha() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        int codeLength = CaptchaConfigEnum.CODE_LENGTH.getValue();
        int randomBound = (int) Math.pow(10, codeLength);
        String code = String.format("%0" + codeLength + "d", secureRandom.nextInt(randomBound));
        String redisKey = buildRedisKey(captchaId);

        boolean saved = redisCache.setString(
                redisKey,
                code,
                CaptchaConfigEnum.EXPIRE_MINUTES.getValue(),
                TimeUnit.MINUTES
        );
        if (!saved) {
            throw new BusinessException(BizMsgEnum.CAPTCHA_GENERATE_FAILED);
        }

        String imageBase64 = CaptchaImageUtil.createBase64Image(
                code,
                CaptchaConfigEnum.IMAGE_WIDTH.getValue(),
                CaptchaConfigEnum.IMAGE_HEIGHT.getValue(),
                CaptchaConfigEnum.NOISE_LINE_COUNT.getValue()
        );

        return CaptchaVO.builder()
                .captchaId(captchaId)
                .imageBase64(imageBase64)
                .build();
    }

    /**
     * 校验并删除验证码。
     *
     * @param captchaId 验证码唯一标识
     * @param code      用户输入的验证码
     */
    @Override
    public void validateCaptcha(String captchaId, String code) {
        if (captchaId == null || captchaId.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException(BizMsgEnum.CAPTCHA_EMPTY);
        }

        String redisKey = buildRedisKey(captchaId.trim());
        String cachedCode = redisCache.getAndDelete(redisKey);
        if (cachedCode == null || !cachedCode.equals(code.trim())) {
            throw new BusinessException(BizMsgEnum.CAPTCHA_EXPIRED_OR_INVALID);
        }
    }

    /**
     * 构建验证码 Redis 缓存键。
     *
     * @param captchaId 验证码唯一标识
     * @return Redis 缓存键
     */
    private String buildRedisKey(String captchaId) {
        return CaptchaRedisKeyEnum.PREFIX.getPrefix() + captchaId;
    }
}
