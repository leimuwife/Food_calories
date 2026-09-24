package com.nutrition.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis Lua 脚本枚举
 * 集中维护需要原子执行的 Redis 脚本，兼容不支持新命令的旧版本 Redis。
 */
@Getter
@RequiredArgsConstructor
public enum RedisScriptEnum {

    /** 原子获取并删除字符串，键不存在时返回 null */
    GET_AND_DELETE("""
            local value = redis.call('GET', KEYS[1])
            if value then
                redis.call('DEL', KEYS[1])
            end
            return value
            """);

    /** Lua 脚本内容 */
    private final String script;
}
