package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.Admin;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.mapper.AdminMapper;
import com.nutrition.service.AdminService;
import com.nutrition.util.JwtUtil;
import com.nutrition.vo.AdminLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 管理员服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 管理员登录
     * 执行流程：
     * 1. 根据 username 查询管理员（自动过滤已删除账号）
     * 2. 账号不存在：抛出异常「账号不存在」
     * 3. 账号禁用：抛出异常「账号已被禁用」
     * 4. 密码校验：使用 BCrypt 比对
     * 5. 生成 JWT Token
     * 6. 返回脱敏信息（不包含密码）
     *
     * @param username 账号
     * @param password 密码
     * @return 登录结果
     */
    @Override
    public AdminLoginVO login(String username, String password) {
        Admin admin = this.getOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));

        if (admin == null) {
            log.warn("管理员登录失败：账号不存在，username={}", username);
            throw new BusinessException(BizMsgEnum.ADMIN_NOT_EXIST);
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            log.warn("管理员登录失败：密码错误，username={}", username);
            throw new BusinessException(BizMsgEnum.ADMIN_PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(String.valueOf(admin.getId()), admin.getUsername());

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setFileIds(admin.getFileIds());
        vo.setPhone(admin.getPhone());

        log.info("管理员登录成功：username={}, id={}", username, admin.getId());

        return vo;
    }
}