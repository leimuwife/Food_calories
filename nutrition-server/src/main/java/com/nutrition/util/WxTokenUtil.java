package com.nutrition.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutrition.config.WxConfigProperties;
import com.nutrition.exception.WxBusinessException;
import com.nutrition.exception.WxConfigException;
import com.nutrition.exception.WxNetworkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 微信 Access Token 工具类
 * 封装 token 获取、缓存、自动刷新逻辑，支持线程安全的并发调用
 * 
 * 缓存策略：默认使用 Redis 缓存，可扩展为本地缓存或其他缓存实现
 * 锁机制：使用全局单锁保证缓存失效时只有一个线程去刷新 token
 *        适用场景：单机低流量服务；高并发场景可优化为分段锁或分布式锁
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WxTokenUtil {

    private final WxConfigProperties wxConfigProperties;
    private final ObjectMapper objectMapper;
    private final RedisCache redisCache;
    private final RestTemplate restTemplate;

    /**
     * Redis 缓存键
     */
    private static final String REDIS_KEY_WX_TOKEN = "wx:access_token";

    /**
     * 全局刷新锁（单锁，缓存击穿防护）
     */
    private final ReentrantLock refreshLock = new ReentrantLock();

    /**
     * 自旋等待间隔时间（毫秒）
     */
    private static final long SPIN_WAIT_INTERVAL_MS = 100;

    /**
     * 最大等待时间（毫秒）
     */
    private static final long MAX_WAIT_TIMEOUT_MS = 5000;

    /**
     * HTTP 请求最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 2;

    /**
     * 缓存最小 TTL（秒），防止配置偏移量过大导致 TTL 为负数
     */
    private static final int MIN_CACHE_TTL_SECONDS = 60;

    /**
     * 微信 access_token 获取接口 URL
     */
    private static final String WX_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    /**
     * Token 过期错误码
     */
    private static final int ERR_CODE_TOKEN_EXPIRED = 40001;
    private static final int ERR_CODE_TOKEN_INVALID = 42001;

    /**
     * 获取 access_token
     * 优先从缓存获取，缓存失效时自动刷新
     *
     * @return access_token
     * @throws WxConfigException   配置缺失时抛出
     * @throws WxNetworkException  网络异常时抛出
     * @throws WxBusinessException 微信业务错误时抛出
     */
    public String getAccessToken() {
        validateConfig();

        String token = redisCache.getString(REDIS_KEY_WX_TOKEN);
        if (token != null && !token.isEmpty()) {
            return token;
        }

        return refreshAccessToken();
    }

    /**
     * 刷新 access_token（内部方法）
     * 保证缓存失效瞬间只有一个线程去刷新，其余线程自旋等待
     *
     * @return 新的 access_token
     */
    public String refreshAccessToken() {
        if (refreshLock.tryLock()) {
            try {
                String cachedToken = redisCache.getString(REDIS_KEY_WX_TOKEN);
                if (cachedToken != null && !cachedToken.isEmpty()) {
                    return cachedToken;
                }
                return doRefreshAccessToken();
            } finally {
                refreshLock.unlock();
            }
        } else {
            return waitForTokenRefresh();
        }
    }

    /**
     * 强制刷新 access_token
     * 主动清空缓存并重新获取，用于上层捕获到 token 过期错误时调用
     *
     * @return 新的 access_token
     */
    public String forceRefreshAccessToken() {
        validateConfig();

        if (refreshLock.tryLock()) {
            try {
                redisCache.delete(REDIS_KEY_WX_TOKEN);
                return doRefreshAccessToken();
            } finally {
                refreshLock.unlock();
            }
        } else {
            return waitForTokenRefresh();
        }
    }

    /**
     * 检查 token 是否有效
     *
     * @return true 表示有效，false 表示需要刷新
     */
    public boolean isTokenValid() {
        String token = redisCache.getString(REDIS_KEY_WX_TOKEN);
        return token != null && !token.isEmpty();
    }

    /**
     * 判断错误码是否表示 token 过期
     *
     * @param errorCode 微信接口错误码
     * @return true 表示 token 过期
     */
    public boolean isTokenExpired(int errorCode) {
        return errorCode == ERR_CODE_TOKEN_EXPIRED || errorCode == ERR_CODE_TOKEN_INVALID;
    }

    /**
     * 校验微信配置
     *
     * @throws WxConfigException 配置缺失时抛出
     */
    private void validateConfig() {
        String appId = wxConfigProperties.getAppId();
        String appSecret = wxConfigProperties.getAppSecret();

        if (appId == null || appId.isEmpty()) {
            throw new WxConfigException("微信小程序 AppID 未配置，请设置环境变量 WX_MINI_APPID");
        }
        if (appSecret == null || appSecret.isEmpty()) {
            throw new WxConfigException("微信小程序 AppSecret 未配置，请设置环境变量 WX_MINI_APPSECRET");
        }
    }

    /**
     * 执行实际的 token 刷新操作
     */
    private String doRefreshAccessToken() {
        String appId = wxConfigProperties.getAppId();
        String appSecret = wxConfigProperties.getAppSecret();
        String url = String.format(WX_TOKEN_URL, appId, appSecret);

        log.info("开始刷新微信 access_token, appId={}", maskAppId(appId));

        long startTime = System.currentTimeMillis();
        String response;
        try {
            response = executeWithRetry(url);
        } catch (RestClientException e) {
            throw new WxNetworkException("调用微信 token 接口失败", e);
        }
        long duration = System.currentTimeMillis() - startTime;
        log.debug("微信 token 接口响应耗时: {}ms", duration);

        if (response == null || response.isEmpty()) {
            throw new WxNetworkException("微信 token 接口返回空响应");
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("解析微信 token 响应失败, response={}", response);
            throw new WxNetworkException("解析微信 token 响应失败", e);
        }

        if (json.has("errcode")) {
            int errCode = json.get("errcode").asInt();
            if (errCode != 0) {
                String errMsg = json.has("errmsg") ? json.get("errmsg").asText() : "未知错误";
                log.error("获取微信 access_token 失败: errcode={}, errmsg={}, appId={}", errCode, errMsg, maskAppId(appId));
                throw new WxBusinessException(errCode, "获取微信 access_token 失败: " + errMsg);
            }
        }

        if (!json.has("access_token")) {
            throw new WxNetworkException("微信 token 接口响应缺少 access_token 字段");
        }

        String accessToken = json.get("access_token").asText();
        int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 7200;

        int ttlSeconds = expiresIn - wxConfigProperties.getTokenRefreshSeconds();
        if (ttlSeconds < MIN_CACHE_TTL_SECONDS) {
            log.warn("计算的缓存 TTL({}秒)小于最小兜底值，强制设置为{}秒", ttlSeconds, MIN_CACHE_TTL_SECONDS);
            ttlSeconds = MIN_CACHE_TTL_SECONDS;
        }

        redisCache.setString(REDIS_KEY_WX_TOKEN, accessToken, ttlSeconds, TimeUnit.SECONDS);
        log.info("微信 access_token 刷新成功, 缓存有效期: {}秒, 耗时: {}ms", ttlSeconds, duration);

        return accessToken;
    }

    /**
     * 自旋等待其他线程刷新 token
     */
    private String waitForTokenRefresh() {
        long startTime = System.currentTimeMillis();
        int spinCount = 0;

        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIMEOUT_MS) {
            String token = redisCache.getString(REDIS_KEY_WX_TOKEN);
            if (token != null && !token.isEmpty()) {
                log.debug("自旋等待 token 成功, 等待次数={}, 耗时={}ms", spinCount, System.currentTimeMillis() - startTime);
                return token;
            }

            spinCount++;
            try {
                Thread.sleep(SPIN_WAIT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WxNetworkException("等待 token 刷新被中断", e);
            }
        }

        log.error("等待 token 刷新超时, 最大等待时间={}ms", MAX_WAIT_TIMEOUT_MS);
        throw new WxNetworkException("获取 access_token 超时");
    }

    /**
     * 带重试的 HTTP GET 请求
     */
    private String executeWithRetry(String url) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                return restTemplate.getForObject(url, String.class);
            } catch (RestClientException e) {
                lastException = e;
                retryCount++;
                if (retryCount <= MAX_RETRY_COUNT) {
                    log.warn("微信接口请求失败，第{}次重试, url={}", retryCount, url);
                    try {
                        Thread.sleep(100L * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }

        throw new RestClientException("微信接口请求失败，已重试" + MAX_RETRY_COUNT + "次", lastException);
    }

    /**
     * 对 AppId 进行脱敏处理
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 8) {
            return "******";
        }
        return appId.substring(0, 4) + "******" + appId.substring(appId.length() - 4);
    }
}
