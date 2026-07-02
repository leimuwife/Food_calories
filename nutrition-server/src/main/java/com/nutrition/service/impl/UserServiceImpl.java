package com.nutrition.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.param.GoalUpdateParam;
import com.nutrition.param.LoginParam;
import com.nutrition.param.ProfileUpdateParam;
import com.nutrition.param.RegisterParam;
import com.nutrition.entity.SysUser;
import com.nutrition.mapper.SysUserMapper;
import com.nutrition.service.UserService;
import com.nutrition.util.JwtUtil;
import com.nutrition.vo.LoginResultVO;
import com.nutrition.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户业务层实现类
 * 负责用户注册、登录、个人信息管理等功能
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResultVO login(LoginParam param) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, param.getUsername()));
        if (user == null) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(param.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUser(convertToVO(user));
        return result;
    }

    @Override
    @Transactional
    public LoginResultVO register(RegisterParam param) {
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, param.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(param.getUsername());
        user.setNickname(param.getNickname());
        user.setPasswordHash(passwordEncoder.encode(param.getPassword()));
        user.setDailyCalorieGoal(2000);
        user.setDailyProteinGoal(60);
        user.setDailyFatGoal(55);
        user.setDailyCarbsGoal(250);

        this.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUser(convertToVO(user));
        return result;
    }

    @Override
    public LoginResultVO wxLogin(String code) {
        String mockOpenid = "wx_" + code;
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOpenid, mockOpenid));

        if (user == null) {
            user = new SysUser();
            user.setOpenid(mockOpenid);
            user.setNickname("微信用户");
            user.setDailyCalorieGoal(2000);
            user.setDailyProteinGoal(60);
            user.setDailyFatGoal(55);
            user.setDailyCarbsGoal(250);
            this.save(user);
        }

        String token = jwtUtil.generateToken(user.getId(),
                StrUtil.blankToDefault(user.getUsername(), "wx_user"));
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUser(convertToVO(user));
        return result;
    }

    @Override
    public SysUser getCurrentUser(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateParam updateInfo) {
        SysUser user = this.getById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        if (StrUtil.isNotBlank(updateInfo.getNickname())) {
            user.setNickname(updateInfo.getNickname());
        }
        if (StrUtil.isNotBlank(updateInfo.getAvatar())) {
            user.setAvatar(updateInfo.getAvatar());
        }
        if (StrUtil.isNotBlank(updateInfo.getEmail())) {
            user.setEmail(updateInfo.getEmail());
        }
        this.updateById(user);
    }

    @Override
    public void updateGoals(Long userId, GoalUpdateParam goals) {
        SysUser user = this.getById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        if (goals.getDailyCalorieGoal() != null) user.setDailyCalorieGoal(goals.getDailyCalorieGoal());
        if (goals.getDailyProteinGoal() != null) user.setDailyProteinGoal(goals.getDailyProteinGoal());
        if (goals.getDailyFatGoal() != null) user.setDailyFatGoal(goals.getDailyFatGoal());
        if (goals.getDailyCarbsGoal() != null) user.setDailyCarbsGoal(goals.getDailyCarbsGoal());

        this.updateById(user);
    }

    @Override
    public UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setDailyCalorieGoal(user.getDailyCalorieGoal());
        vo.setDailyProteinGoal(user.getDailyProteinGoal());
        vo.setDailyFatGoal(user.getDailyFatGoal());
        vo.setDailyCarbsGoal(user.getDailyCarbsGoal());
        return vo;
    }
}
