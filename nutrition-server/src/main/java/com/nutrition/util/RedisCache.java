package com.nutrition.util;

import com.nutrition.config.RedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 提供通用的缓存操作方法，使用StringRedisTemplate手动序列化
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisConfig.CacheConfigProperties cacheConfigProperties;

    public static final String PREFIX_BLACKLIST = "blacklist:";
    public static final String PREFIX_USER = "user:";
    public static final String PREFIX_DIET = "diet:";
    public static final String PREFIX_FOOD = "food:";
    public static final String PREFIX_MOMENT_LIKE_USER = "moment:like:user:";
    public static final String PREFIX_MOMENT_LIKE_COUNT = "moment:like:count:";
    public static final String PREFIX_MOMENT_COMMENT_COUNT = "moment:comment:count:";

    /**
     * 构建黑名单缓存键
     *
     * @param token 令牌
     * @return 缓存键
     */
    public static String getBlacklistKey(String token) {
        return PREFIX_BLACKLIST + token;
    }

    /**
     * 构建用户缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String getUserKey(Long userId) {
        return PREFIX_USER + userId;
    }

    /**
     * 构建饮食记录缓存键
     *
     * @param userId 用户ID
     * @param date   日期
     * @return 缓存键
     */
    public static String getDietKey(Long userId, String date) {
        return PREFIX_DIET + userId + ":" + date;
    }

    /**
     * 构建食物缓存键
     *
     * @param keyword 关键词
     * @return 缓存键
     */
    public static String getFoodKey(String keyword) {
        return PREFIX_FOOD + keyword;
    }

    /**
     * 构建动态点赞防重键
     *
     * @param feedId 动态ID
     * @param userId 用户ID
     * @return 缓存键
     */
    public static String getMomentLikeUserKey(Long feedId, Long userId) {
        return PREFIX_MOMENT_LIKE_USER + feedId + "_" + userId;
    }

    /**
     * 构建动态点赞计数键
     *
     * @param feedId 动态ID
     * @return 缓存键
     */
    public static String getMomentLikeCountKey(Long feedId) {
        return PREFIX_MOMENT_LIKE_COUNT + feedId;
    }

    /**
     * 构建动态评论计数键
     *
     * @param feedId 动态ID
     * @return 缓存键
     */
    public static String getMomentCommentCountKey(Long feedId) {
        return PREFIX_MOMENT_COMMENT_COUNT + feedId;
    }

    /**
     * 原子自增操作
     *
     * @param key 缓存键
     * @return 自增后的值
     */
    public long increment(String key) {
        try {
            return stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("Redis自增失败: key={}, error={}", key, e.getMessage());
            return 0;
        }
    }

    /**
     * 原子自减操作
     *
     * @param key 缓存键
     * @return 自减后的值
     */
    public long decrement(String key) {
        try {
            return stringRedisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.warn("Redis自减失败: key={}, error={}", key, e.getMessage());
            return 0;
        }
    }

    /**
     * SETNX 原子操作（键不存在时设置）
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否设置成功（true=键不存在，已设置；false=键已存在）
     */
    public boolean setIfAbsent(String key, String value) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, value));
        } catch (Exception e) {
            log.warn("Redis SETNX失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * SCAN 游标分批非阻塞扫描匹配的键（生产环境推荐使用）
     * 底层基于 Redis SCAN 命令实现，不会阻塞 Redis 主线程
     * 使用 try-with-resources 自动关闭游标释放连接
     *
     * @param pattern 键匹配模式（如 "moment:like:count:*"）
     * @return 匹配的键集合（已去重）
     */
    public Set<String> scanKeys(String pattern) {
        Set<String> result = new HashSet<>();
        try (var cursor = stringRedisTemplate.executeWithStickyConnection(
                connection -> connection.scan(ScanOptions.scanOptions()
                        .match(pattern)
                        .count(500)
                        .build()))) {
            while (cursor.hasNext()) {
                result.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.error("Redis SCAN扫描失败: pattern={}, error={}", pattern, e.getMessage(), e);
        }
        return result;
    }

    /**
     * 获取所有匹配的键（已废弃，生产环境禁止使用）
     * KEYS命令会阻塞Redis主线程，键量大时会造成服务卡死
     * 请使用 scanKeys() 方法替代
     *
     * @param pattern 键匹配模式
     * @return 匹配的键集合
     * @deprecated 推荐使用 scanKeys(String pattern)
     */
    @Deprecated
    public java.util.Set<String> keys(String pattern) {
        log.warn("警告：使用了已废弃的 keys() 方法，请改用 scanKeys()");
        try {
            return stringRedisTemplate.keys(pattern);
        } catch (Exception e) {
            log.warn("Redis keys扫描失败: pattern={}, error={}", pattern, e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值（对象）
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis设置缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 设置缓存（不带过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值（对象）
     */
    public void set(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json);
        } catch (Exception e) {
            log.warn("Redis设置缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取缓存
     *
     * @param key   缓存键
     * @param clazz 返回值类型
     * @param <T>   返回值泛型
     * @return 缓存对象，不存在返回null
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
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
            stringRedisTemplate.delete(key);
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
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis检查缓存存在失败: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            stringRedisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis设置过期时间失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 设置字符串缓存（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值（字符串）
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void setString(String key, String value, long timeout, TimeUnit unit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis设置字符串缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 获取字符串缓存
     *
     * @param key 缓存键
     * @return 字符串值，不存在返回null
     */
    public String getString(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis获取字符串缓存失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 获取用户缓存过期时间（秒）
     *
     * @return 过期时间（秒）
     */
    public long getUserCacheTtlSeconds() {
        String ttl = cacheConfigProperties.getConfig("user");
        return parseTtlSeconds(ttl, 30 * 60);
    }

    /**
     * 获取饮食记录缓存过期时间（秒）
     *
     * @return 过期时间（秒）
     */
    public long getDietCacheTtlSeconds() {
        String ttl = cacheConfigProperties.getConfig("diet");
        return parseTtlSeconds(ttl, 30 * 60);
    }

    /**
     * 获取黑名单缓存过期时间（秒）
     *
     * @return 过期时间（秒）
     */
    public long getBlacklistCacheTtlSeconds() {
        String ttl = cacheConfigProperties.getConfig("blacklist");
        return parseTtlSeconds(ttl, 7 * 24 * 60 * 60);
    }

    /**
     * 解析TTL字符串为秒数
     * 支持格式：30s, 1m, 30m, 1h, 7d
     *
     * @param ttl           TTL字符串
     * @param defaultSeconds 默认秒数
     * @return 解析后的秒数
     */
    private long parseTtlSeconds(String ttl, long defaultSeconds) {
        if (ttl == null || ttl.isBlank()) {
            return defaultSeconds;
        }
        ttl = ttl.trim().toLowerCase();
        try {
            char unit = ttl.charAt(ttl.length() - 1);
            long value = Long.parseLong(ttl.substring(0, ttl.length() - 1));
            return switch (unit) {
                case 's' -> value;
                case 'm' -> value * 60;
                case 'h' -> value * 60 * 60;
                case 'd' -> value * 24 * 60 * 60;
                default -> defaultSeconds;
            };
        } catch (Exception e) {
            return defaultSeconds;
        }
    }
}
