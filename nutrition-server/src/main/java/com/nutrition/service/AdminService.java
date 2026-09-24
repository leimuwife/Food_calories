package com.nutrition.service;

import com.nutrition.vo.AdminLoginVO;
import com.nutrition.vo.AdminUserVO;
import com.nutrition.vo.PageVO;

/**
 * 管理员服务接口
 */
public interface AdminService {

    /**
     * 管理员登录
     *
     * @param username 账号
     * @param password 密码
     * @return 登录结果（Token + 管理员信息）
     */
    AdminLoginVO login(String username, String password);

    /**
     * 分页查询全部注册用户。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 用户分页结果
     */
    PageVO<AdminUserVO> listUsers(int pageNum, int pageSize);

    /**
     * 启用或禁用用户。
     *
     * @param userId  用户ID
     * @param enabled true启用，false禁用
     * @return 更新后的用户信息
     */
    AdminUserVO updateUserStatus(Long userId, boolean enabled);
}