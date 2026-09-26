package com.nutrition.util;

import com.nutrition.client.FastApiProperties;
import com.nutrition.common.BusinessException;
import com.nutrition.enums.BizMsgEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 服务间回调鉴权工具。
 * <p>
 * Python AI 服务回调 Java 接口时使用 {@code Authorization: Bearer <FASTAPI_SECRET_KEY>}，
 * 此处统一校验，避免回调接口被匿名调用。
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackAuthUtil {

    private final FastApiProperties fastApiProperties;

    /**
     * 校验回调请求头中的 API Key，失败抛出 {@link BusinessException}。
     *
     * @param authHeader 请求头 Authorization 的值
     */
    public void validate(String authHeader) {
        String expected = fastApiProperties.getApiSecretKey();
        if (expected == null || expected.trim().isEmpty()) {
            log.error("回调密钥未配置，请设置 FASTAPI_SECRET_KEY");
            throw new BusinessException(BizMsgEnum.CALLBACK_API_KEY_NOT_CONFIGURED);
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(BizMsgEnum.CALLBACK_UNAUTHORIZED);
        }
        String token = authHeader.substring(7).trim();
        // 常量时间比较，避免时序侧信道
        boolean matched = MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
        if (!matched) {
            throw new BusinessException(BizMsgEnum.CALLBACK_UNAUTHORIZED);
        }
    }
}
