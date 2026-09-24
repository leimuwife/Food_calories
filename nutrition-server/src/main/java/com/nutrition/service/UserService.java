package com.nutrition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nutrition.param.LoginParam;
import com.nutrition.param.ProfileUpdateParam;
import com.nutrition.param.RegisterParam;
import com.nutrition.entity.SysUser;
import com.nutrition.vo.LoginResultVO;
import com.nutrition.vo.UserVO;

public interface UserService extends IService<SysUser> {

    LoginResultVO login(LoginParam param);

    /**
     * 注册普通用户账号。
     *
     * @param param 注册参数，包含图形验证码和两次密码
     */
    void register(RegisterParam param);

    SysUser getCurrentUser(Long userId);

    void updateProfile(Long userId, ProfileUpdateParam updateInfo);

    UserVO convertToVO(SysUser user);
}
