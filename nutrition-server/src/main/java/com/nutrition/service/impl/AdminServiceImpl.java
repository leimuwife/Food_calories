package com.nutrition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.entity.Admin;
import com.nutrition.entity.SysUser;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.enums.UserAccountStatusEnum;
import com.nutrition.enums.JwtRoleEnum;
import com.nutrition.mapper.AdminMapper;
import com.nutrition.mapper.SysUserMapper;
import com.nutrition.service.AdminService;
import com.nutrition.util.JwtUtil;
import com.nutrition.vo.AdminLoginVO;
import com.nutrition.vo.AdminUserVO;
import com.nutrition.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理员服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

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

        String token = jwtUtil.generateToken(String.valueOf(admin.getId()), admin.getUsername(), JwtRoleEnum.ADMIN);

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

    /**
     * 分页查询全部注册用户，包含已禁用用户。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    @Override
    public PageVO<AdminUserVO> listUsers(int pageNum, int pageSize) {
        int currentPage = Math.max(pageNum, 1);
        int currentPageSize = Math.max(pageSize, 1);
        long total = sysUserMapper.countAllUsers();
        int offset = (currentPage - 1) * currentPageSize;
        List<AdminUserVO> records = sysUserMapper.selectUserPage(offset, currentPageSize)
                .stream()
                .map(this::convertToAdminUserVO)
                .toList();
        return new PageVO<>(records, total, currentPage, currentPageSize);
    }

    /**
     * 启用或禁用用户。
     *
     * @param userId  用户ID
     * @param enabled true启用，false禁用
     * @return 更新后的用户信息
     */
    @Override
    @Transactional
    public AdminUserVO updateUserStatus(Long userId, boolean enabled) {
        if (userId == null) {
            throw new BusinessException(BizMsgEnum.USER_NOT_EXIST);
        }
        SysUser user = sysUserMapper.selectUserByIdIncludingDisabled(userId);
        if (user == null) {
            throw new BusinessException(BizMsgEnum.USER_NOT_EXIST);
        }
        int status = enabled
                ? UserAccountStatusEnum.ENABLED.getValue()
                : UserAccountStatusEnum.DISABLED.getValue();
        sysUserMapper.updateUserStatus(userId, status);
        user.setDeleteFlag(status);
        log.info("管理员修改用户状态: userId={}, enabled={}", userId, enabled);
        return convertToAdminUserVO(user);
    }

    /**
     * 转换为管理端用户视图对象。
     *
     * @param user 用户实体
     * @return 管理端用户信息
     */
    private AdminUserVO convertToAdminUserVO(SysUser user) {
        int status = user.getDeleteFlag() == null
                ? UserAccountStatusEnum.ENABLED.getValue()
                : user.getDeleteFlag();
        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .deleteFlag(status)
                .enabled(status == UserAccountStatusEnum.ENABLED.getValue())
                .build();
    }
}
