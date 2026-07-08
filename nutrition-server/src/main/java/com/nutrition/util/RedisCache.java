package com.nutrition.util;

import com.nutrition.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 提供通用的缓存操作方法，支持String类型和Object类型
 * 支持从配置文件读取各缓存区域的过期时间
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig.CacheConfigProperties cacheConfigProperties;

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis设置缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 设置缓存（永久有效）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.warn("Redis设置缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return (T) value;
        } catch (Exception e) {
            log.warn("Redis获取缓存失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 获取缓存（通用Object类型）
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回null
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis获取缓存失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis删除缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis检查缓存失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis设置过期时间失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 缓存键前缀常量
     */
    public static final String PREFIX_BLACKLIST = "blacklist:";
    public static final String PREFIX_USER = "user:";
    public static final String PREFIX_DIET = "diet:";
    public static final String PREFIX_FEED = "feed:";
    public static final String PREFIX_CHECKIN = "checkin:";

    /**
     * 生成Token黑名单键
     */
    public static String getBlacklistKey(String token) {
        return PREFIX_BLACKLIST + token;
    }

    /**
     * 生成用户信息键
     */
    public static String getUserKey(Long userId) {
        return PREFIX_USER + userId;
    }

    /**
     * 生成用户信息键（字符串ID）
     */
    public static String getUserKey(String userId) {
        return PREFIX_USER + userId;
    }

    /**
     * 生成每日饮食统计键
     */
    public static String getDietKey(Long userId, String date) {
        return PREFIX_DIET + userId + ":" + date;
    }

    /**
     * 生成轻友圈列表键
     */
    public static String getFeedKey(int page) {
        return PREFIX_FEED + "list:page:" + page;
    }

    /**
     * 生成打卡状态键
     */
    public static String getCheckinKey(Long userId, int year, int month) {
        return PREFIX_CHECKIN + userId + ":" + year + ":" + month;
    }

    /**
     * 获取用户信息缓存过期时间（秒）
     */
    public long getUserCacheTtlSeconds() {
        if (cacheConfigProperties.getUser() != null) {
            return parseTtl(cacheConfigProperties.getUser().getTtl()).getSeconds();
        }
        return Duration.ofMinutes(30).getSeconds();
    }

    /**
     * 获取每日饮食缓存过期时间（秒）
     */
    public long getDietCacheTtlSeconds() {
        if (cacheConfigProperties.getDiet() != null) {
            return parseTtl(cacheConfigProperties.getDiet().getTtl()).getSeconds();
        }
        return Duration.ofMinutes(30).getSeconds();
    }

    /**
     * 获取黑名单缓存过期时间（秒）
     */
    public long getBlacklistCacheTtlSeconds() {
        if (cacheConfigProperties.getBlacklist() != null) {
            return parseTtl(cacheConfigProperties.getBlacklist().getTtl()).getSeconds();
        }
        return Duration.ofDays(7).getSeconds();
    }

    /**
     * 解析Spring风格的时长字符串
     * 支持格式：30s, 1m, 30m, 1h, 7d
     * 也支持ISO-8601格式：PT30M, PT1H, P7D
     */
    private Duration parseTtl(String ttl) {
        if (ttl == null || ttl.isBlank()) {
            return Duration.ofMinutes(30);
        }
        ttl = ttl.trim().toLowerCase();
        try {
            char unit = ttl.charAt(ttl.length() - 1);
            long value = Long.parseLong(ttl.substring(0, ttl.length() - 1));
            return switch (unit) {
                case 's' -> Duration.ofSeconds(value);
                case 'm' -> Duration.ofMinutes(value);
                case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value);
                default -> Duration.parse(ttl);
            };
        } catch (Exception e) {
            try {
                return Duration.parse(ttl);
            } catch (Exception ex) {
                return Duration.ofMinutes(30);
            }
        }
    }
}