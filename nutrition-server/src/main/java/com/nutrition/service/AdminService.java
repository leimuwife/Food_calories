package com.nutrition.service;

import com.nutrition.vo.AdminLoginVO;

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
}