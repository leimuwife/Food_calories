package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.param.AdminUserStatusParam;
import com.nutrition.service.AdminService;
import com.nutrition.vo.AdminLoginVO;
import com.nutrition.vo.AdminUserVO;
import com.nutrition.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
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

    /**
     * 分页查询所有注册用户。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数，默认10
     * @return 用户分页结果
     */
    @GetMapping("/users")
    @Operation(summary = "用户分页列表", description = "管理员分页查询全部注册用户")
    public Result<PageVO<AdminUserVO>> listUsers(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return Result.ok(adminService.listUsers(pageNum, pageSize));
    }

    /**
     * 启用或禁用用户。
     *
     * @param userId 用户ID
     * @param param  状态参数
     * @return 更新后的用户信息
     */
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "启用或禁用用户", description = "禁用后 delete_flag=1，用户无法登录")
    public Result<AdminUserVO> updateUserStatus(@PathVariable("userId") Long userId,
                                                 @Valid @RequestBody AdminUserStatusParam param) {
        return Result.ok(adminService.updateUserStatus(userId, param.getEnabled()));
    }
}