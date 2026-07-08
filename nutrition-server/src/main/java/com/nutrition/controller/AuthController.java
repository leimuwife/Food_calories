package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.config.JwtAuthFilter;
import com.nutrition.param.LoginParam;
import com.nutrition.param.RegisterParam;
import com.nutrition.param.WxLoginParam;
import com.nutrition.service.UserService;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.LoginResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 用户认证控制器
 * 处理登录、注册、微信登录、登出等认证相关请求
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;
    private final RedisCache redisCache;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginParam param) {
        LoginResultVO result = userService.login(param);
        return Result.ok("登录成功", result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<LoginResultVO> register(@Valid @RequestBody RegisterParam param) {
        LoginResultVO result = userService.register(param);
        return Result.ok("注册成功", result);
    }

    /**
     * 微信登录
     */
    @PostMapping("/wx-login")
    public Result<LoginResultVO> wxLogin(@Valid @RequestBody WxLoginParam param) {
        LoginResultVO result = userService.wxLogin(param.getCode());
        return Result.ok("登录成功", result);
    }

    /**
     * 用户登出
     * 将当前Token加入黑名单，立即失效
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtAuthFilter.blacklistToken(token, System.currentTimeMillis() + 604800000L);
            
            Long userId = (Long) request.getAttribute("userId");
            if (userId != null) {
                String userKey = RedisCache.getUserKey(userId);
                redisCache.delete(userKey);
                log.debug("用户{}登出，已清除用户信息缓存", userId);
            }
        }
        return Result.ok("登出成功");
    }
}