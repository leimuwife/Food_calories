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

    LoginResultVO register(RegisterParam param);

    LoginResultVO wxLogin(String code);

    SysUser getCurrentUser(Long userId);

    void updateProfile(Long userId, ProfileUpdateParam updateInfo);

    UserVO convertToVO(SysUser user);
}
