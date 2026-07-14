package com.nutrition.config;

import com.nutrition.util.JwtUtil;
import com.nutrition.util.RedisCache;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器
 * 校验请求头中的 Authorization: Bearer <token>
 * 支持Token黑名单机制，用户登出后Token立即失效
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final RedisCache redisCache;

    /** 无需认证的路径 */
    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/wx-login",
            "/api/auth/logout",
            "/api/attachment/",
            "/api/admin/login",
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // 从 Header 中获取 Token
        String authHeader = req.getHeader("Authorization");
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");

        // 白名单路径：有 token 则尝试解析，无 token 直接放行
        for (String white : WHITE_LIST) {
            if (path.startsWith(white)) {
                if (hasToken) {
                    tryParseToken(req, authHeader);
                }
                chain.doFilter(request, response);
                return;
            }
        }

        // 非白名单路径：必须有有效 token
        if (!hasToken) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"未登录或Token缺失\",\"data\":null}");
            return;
        }

        String token = authHeader.substring(7);

        // 检查Token是否在黑名单中
        String blacklistKey = RedisCache.getBlacklistKey(token);
        if (redisCache.exists(blacklistKey)) {
            log.debug("Token已被拉黑: {}", token.substring(0, 20) + "...");
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"Token已失效，请重新登录\",\"data\":null}");
            return;
        }

        // 验证Token有效性
        if (!jwtUtil.validateToken(token)) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            return;
        }

        // 将 userId 存入请求属性，供 Controller 使用
        Long userId = jwtUtil.getUserIdFromToken(token);
        req.setAttribute("userId", userId);

        chain.doFilter(request, response);
    }

    /**
     * 尝试解析 Token，失败不拦截
     */
    private void tryParseToken(HttpServletRequest req, String authHeader) {
        try {
            String token = authHeader.substring(7);
            String blacklistKey = RedisCache.getBlacklistKey(token);
            if (redisCache.exists(blacklistKey)) {
                return;
            }
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                req.setAttribute("userId", userId);
            }
        } catch (Exception e) {
            log.debug("白名单路径Token解析失败，继续放行: {}", e.getMessage());
        }
    }

    /**
     * 将Token加入黑名单
     *
     * @param token     Token字符串
     * @param expiresAt 过期时间（毫秒）
     */
    public void blacklistToken(String token, long expiresAt) {
        String key = RedisCache.getBlacklistKey(token);
        long ttl = expiresAt - System.currentTimeMillis();
        long maxTtlSeconds = redisCache.getBlacklistCacheTtlSeconds();
        if (ttl > 0) {
            long ttlSeconds = Math.min(ttl / 1000, maxTtlSeconds);
            redisCache.set(key, 1, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Token已加入黑名单，剩余有效期: {}秒", ttlSeconds);
        }
    }
}