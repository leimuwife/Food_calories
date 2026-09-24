package com.nutrition.util;

import com.nutrition.enums.JwtRoleEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - 统一生成与校验令牌
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成普通用户 JWT 令牌。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(String userId, String username) {
        return generateToken(userId, username, JwtRoleEnum.USER);
    }

    /**
     * 生成带角色的 JWT 令牌。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色枚举
     * @return JWT令牌
     */
    public String generateToken(String userId, String username, JwtRoleEnum role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role.getCode());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从令牌中解析角色。
     *
     * @param token JWT令牌
     * @return 角色编码；旧令牌未携带角色时返回 null
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /** 从令牌中解析用户 ID */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /** 校验令牌是否有效 */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 获取Token过期时间（毫秒） */
    public long getExpirationTime(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime();
    }

    /** 获取Token有效期（毫秒） */
    public long getExpiration() {
        return expiration;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
