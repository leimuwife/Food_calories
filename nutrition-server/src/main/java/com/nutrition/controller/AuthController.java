package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.param.LoginParam;
import com.nutrition.param.RegisterParam;
import com.nutrition.param.WxLoginParam;
import com.nutrition.service.UserService;
import com.nutrition.vo.LoginResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginParam param) {
        LoginResultVO result = userService.login(param);
        return Result.ok("登录成功", result);
    }

    @PostMapping("/register")
    public Result<LoginResultVO> register(@Valid @RequestBody RegisterParam param) {
        LoginResultVO result = userService.register(param);
        return Result.ok("注册成功", result);
    }

    @PostMapping("/wx-login")
    public Result<LoginResultVO> wxLogin(@Valid @RequestBody WxLoginParam param) {
        LoginResultVO result = userService.wxLogin(param.getCode());
        return Result.ok("登录成功", result);
    }
}
