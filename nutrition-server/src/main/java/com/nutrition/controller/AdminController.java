package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.service.AdminService;
import com.nutrition.vo.AdminLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "管理员管理", description = "管理员登录接口")
public class AdminController {

    private final AdminService adminService;

    /**
     * 管理员登录接口
     * 路径：POST /api/admin/login
     * 入参：username（账号）、password（密码）
     * 登录成功返回 Token 和管理员基础信息
     *
     * @param request 登录请求参数
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员账号密码登录")
    public Result<AdminLoginVO> login(@Validated @RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return Result.badRequest("账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.badRequest("密码不能为空");
        }

        AdminLoginVO result = adminService.login(username.trim(), password);

        return Result.ok("登录成功", result);
    }
}