package com.nutrition.controller;

import com.nutrition.common.Result;
import com.nutrition.entity.SysUser;
import com.nutrition.param.ProfileUpdateParam;
import com.nutrition.service.UserService;
import com.nutrition.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 获取当前用户信息 */
    @GetMapping("/profile")
    public Result<UserVO> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userService.getCurrentUser(userId);
        UserVO vo = userService.convertToVO(user);
        return Result.ok(vo);
    }

    /** 更新个人信息 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(HttpServletRequest request,
                                       @Valid @RequestBody ProfileUpdateParam updateInfo) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, updateInfo);
        return Result.ok("更新成功", null);
    }
}
