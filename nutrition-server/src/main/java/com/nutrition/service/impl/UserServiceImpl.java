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
            user = createMockUser(code, mockOpenid);
            this.save(user);
        }

        String token = jwtUtil.generateToken(user.getId(),
                StrUtil.blankToDefault(user.getUsername(), "wx_user"));
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUser(convertToVO(user));
        return result;
    }

    private SysUser createMockUser(String code, String openid) {
        SysUser user = new SysUser();
        user.setOpenid(openid);

        return switch (code) {
            case "test_code_user1" -> {
                user.setNickname("张三");
                user.setUsername("zhangsan");
                user.setEmail("zhangsan@example.com");
                user.setDailyCalorieGoal(2000);
                user.setDailyProteinGoal(60);
                user.setDailyFatGoal(55);
                user.setDailyCarbsGoal(250);
                yield user;
            }
            case "test_code_user2" -> {
                user.setNickname("李四");
                user.setUsername("lisi");
                user.setEmail("lisi@example.com");
                user.setDailyCalorieGoal(1800);
                user.setDailyProteinGoal(55);
                user.setDailyFatGoal(50);
                user.setDailyCarbsGoal(220);
                yield user;
            }
            case "test_code_nutritionist" -> {
                user.setNickname("小张营养师");
                user.setUsername("nutritionist_zhang");
                user.setEmail("zhang@nutrition.com");
                user.setDailyCalorieGoal(2200);
                user.setDailyProteinGoal(70);
                user.setDailyFatGoal(60);
                user.setDailyCarbsGoal(280);
                yield user;
            }
            default -> {
                user.setNickname("微信用户");
                user.setUsername("wx_user_" + System.currentTimeMillis());
                user.setDailyCalorieGoal(2000);
                user.setDailyProteinGoal(60);
                user.setDailyFatGoal(55);
                user.setDailyCarbsGoal(250);
                yield user;
            }
        };
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
        if (StrUtil.isNotBlank(updateInfo.getFileId())) {
            user.setFileIds(updateInfo.getFileId());
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
        vo.setFileIds(user.getFileIds());
        vo.setEmail(user.getEmail());
        vo.setDailyCalorieGoal(user.getDailyCalorieGoal());
        vo.setDailyProteinGoal(user.getDailyProteinGoal());
        vo.setDailyFatGoal(user.getDailyFatGoal());
        vo.setDailyCarbsGoal(user.getDailyCarbsGoal());
        return vo;
    }
}
