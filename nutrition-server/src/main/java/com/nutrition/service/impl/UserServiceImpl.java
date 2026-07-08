package com.nutrition.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.param.LoginParam;
import com.nutrition.param.ProfileUpdateParam;
import com.nutrition.param.RegisterParam;
import com.nutrition.entity.SysUser;
import com.nutrition.mapper.SysUserMapper;
import com.nutrition.service.UserService;
import com.nutrition.util.JwtUtil;
import com.nutrition.util.RedisCache;
import com.nutrition.vo.LoginResultVO;
import com.nutrition.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 用户业务层实现类
 * 负责用户注册、登录、个人信息管理等功能
 * 支持Redis缓存用户信息，减少数据库查询
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisCache redisCache;

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

        String token = jwtUtil.generateToken(String.valueOf(user.getId()), user.getUsername());
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        UserVO userVO = convertToVO(user);
        result.setUser(userVO);

        cacheUser(user.getId(), userVO);

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

        this.save(user);

        String token = jwtUtil.generateToken(String.valueOf(user.getId()), user.getUsername());
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        UserVO userVO = convertToVO(user);
        result.setUser(userVO);

        cacheUser(user.getId(), userVO);

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

        String token = jwtUtil.generateToken(String.valueOf(user.getId()),
                StrUtil.blankToDefault(user.getUsername(), "wx_user"));
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        UserVO userVO = convertToVO(user);
        result.setUser(userVO);

        cacheUser(user.getId(), userVO);

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
                yield user;
            }
            case "test_code_user2" -> {
                user.setNickname("李四");
                user.setUsername("lisi");
                user.setEmail("lisi@example.com");
                yield user;
            }
            case "test_code_nutritionist" -> {
                user.setNickname("小张营养师");
                user.setUsername("nutritionist_zhang");
                user.setEmail("zhang@nutrition.com");
                yield user;
            }
            default -> {
                user.setNickname("微信用户");
                user.setUsername("wx_user_" + System.currentTimeMillis());
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

    /**
     * 获取用户信息（带缓存）
     *
     * @param userId 用户ID
     * @return 用户VO对象
     */
    public UserVO getUserVO(Long userId) {
        String cacheKey = RedisCache.getUserKey(userId);
        UserVO cachedVO = redisCache.get(cacheKey, UserVO.class);
        if (cachedVO != null) {
            log.debug("用户{}信息命中缓存", userId);
            return cachedVO;
        }

        SysUser user = getCurrentUser(userId);
        UserVO userVO = convertToVO(user);
        cacheUser(userId, userVO);

        return userVO;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateParam updateInfo) {
        SysUser user = this.getById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        if (StrUtil.isNotBlank(updateInfo.getNickname())) {
            user.setNickname(updateInfo.getNickname());
        }
        if (StrUtil.isNotBlank(updateInfo.getEmail())) {
            user.setEmail(updateInfo.getEmail());
        }
        this.updateById(user);

        clearUserCache(userId);
        log.debug("用户{}信息已更新，缓存已清除", userId);
    }

    @Override
    public UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        return vo;
    }

    /**
     * 缓存用户信息
     */
    private void cacheUser(Long userId, UserVO userVO) {
        String cacheKey = RedisCache.getUserKey(userId);
        long ttlSeconds = redisCache.getUserCacheTtlSeconds();
        redisCache.set(cacheKey, userVO, ttlSeconds, TimeUnit.SECONDS);
        log.debug("用户{}信息已缓存，TTL={}秒", userId, ttlSeconds);
    }

    /**
     * 清除用户信息缓存
     */
    private void clearUserCache(Long userId) {
        String cacheKey = RedisCache.getUserKey(userId);
        redisCache.delete(cacheKey);
    }
}