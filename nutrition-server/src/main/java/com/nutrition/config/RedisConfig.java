package com.nutrition.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis配置类
 * 配置RedisTemplate和RedisCacheManager，支持按缓存名设置不同过期时间
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.cache.redis.custom-cache-config")
    public CacheConfigProperties cacheConfigProperties() {
        return new CacheConfigProperties();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                               CacheConfigProperties cacheConfigProperties) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(Duration.ofMinutes(30));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        if (cacheConfigProperties.getUser() != null) {
            cacheConfigs.put("user", defaultConfig.entryTtl(parseTtl(cacheConfigProperties.getUser().getTtl())));
        }
        if (cacheConfigProperties.getDiet() != null) {
            cacheConfigs.put("diet", defaultConfig.entryTtl(parseTtl(cacheConfigProperties.getDiet().getTtl())));
        }
        if (cacheConfigProperties.getBlacklist() != null) {
            cacheConfigs.put("blacklist", defaultConfig.entryTtl(parseTtl(cacheConfigProperties.getBlacklist().getTtl())));
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
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

    @Data
    public static class CacheConfigProperties {
        private CacheItem user;
        private CacheItem diet;
        private CacheItem blacklist;
    }

    @Data
    public static class CacheItem {
        private String ttl;
    }
}