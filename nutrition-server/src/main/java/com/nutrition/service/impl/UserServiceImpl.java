package com.nutrition.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nutrition.common.BusinessException;
import com.nutrition.enums.AuditSceneEnum;
import com.nutrition.enums.AuditSuggestEnum;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.param.LoginParam;
import com.nutrition.param.ProfileUpdateParam;
import com.nutrition.param.RegisterParam;
import com.nutrition.param.ResetPasswordParam;
import com.nutrition.entity.SysUser;
import com.nutrition.mapper.SysUserMapper;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.CaptchaService;
import com.nutrition.service.ContentAuditService;
import com.nutrition.service.UserFeedbackService;
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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户业务层实现类
 * 负责用户注册、登录、个人信息管理等功能
 * 支持Redis缓存用户信息，减少数据库查询
 * 个人信息编辑包含内容安全审核流程
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisCache redisCache;
    private final ContentAuditService contentAuditService;
    private final UserFeedbackService userFeedbackService;
    private final AttachmentService attachmentService;
    private final CaptchaService captchaService;

    /**
     * 使用用户名和密码登录。
     *
     * @param param 登录参数
     * @return 登录令牌和用户信息
     */
    @Override
    public LoginResultVO login(LoginParam param) {
        String username = param.getUsername().trim();
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(BizMsgEnum.USER_LOGIN_FAILED);
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(param.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(BizMsgEnum.USER_LOGIN_FAILED);
        }

        String token = jwtUtil.generateToken(String.valueOf(user.getId()), user.getUsername());
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        UserVO userVO = convertToVO(user);
        result.setUser(userVO);

        cacheUser(user.getId(), userVO);

        return result;
    }

    /**
     * 校验图形验证码并注册普通用户。
     *
     * @param param 注册参数
     */
    @Override
    @Transactional
    public void register(RegisterParam param) {
        if (!param.getPassword().equals(param.getConfirmPassword())) {
            throw new BusinessException(BizMsgEnum.PASSWORD_NOT_MATCH);
        }
        captchaService.validateCaptcha(param.getCaptchaId(), param.getCaptchaCode());

        String username = param.getUsername().trim();
        if (username.length() < 3 || username.length() > 32) {
            throw new BusinessException(BizMsgEnum.USER_NAME_LENGTH_INVALID);
        }
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (count > 0) {
            throw new BusinessException(BizMsgEnum.USER_NAME_EXIST);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(StrUtil.blankToDefault(param.getNickname(), username));
        user.setPasswordHash(passwordEncoder.encode(param.getPassword()));
        user.setPhone(param.getPhone());
        // sys_user 表允许 delete_flag 为 NULL，必须显式写入正常状态，否则逻辑删除查询会过滤新用户
        user.setDeleteFlag(0);

        this.save(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), username);
    }

    /**
     * 通过「用户名 + 注册手机号」校验身份后重置密码。
     * <p>
     * 手机号不唯一，仅作为身份校验因子，必须与用户名对应的账号手机号完全一致；
     * 校验不通过统一返回「手机号错误」，避免泄露用户名是否存在。
     * </p>
     *
     * @param param 重置密码参数
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordParam param) {
        if (!param.getNewPassword().equals(param.getConfirmPassword())) {
            throw new BusinessException(BizMsgEnum.PASSWORD_NOT_MATCH);
        }

        String username = param.getUsername().trim();
        String phone = param.getPhone().trim();

        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        // 用户不存在或手机号不匹配，统一提示「手机号错误」，防止枚举用户名
        if (user == null || user.getPhone() == null || !user.getPhone().equals(phone)) {
            log.warn("重置密码失败：用户名或手机号不匹配, username={}", username);
            throw new BusinessException(BizMsgEnum.PHONE_NOT_MATCH);
        }

        user.setPasswordHash(passwordEncoder.encode(param.getNewPassword()));
        this.updateById(user);
        clearUserCache(user.getId());
        log.info("用户重置密码成功: userId={}, username={}", user.getId(), username);
    }

    @Override
    public SysUser getCurrentUser(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(BizMsgEnum.USER_NOT_EXIST);
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
            if (cachedVO.getAvatarUrl() == null && cachedVO.getFileIds() != null) {
                log.debug("用户{}缓存中没有avatarUrl，尝试解析", userId);
                String avatarUrl = resolveAvatarUrl(cachedVO.getFileIds());
                cachedVO.setAvatarUrl(avatarUrl);
                cacheUser(userId, cachedVO);
            }
            return cachedVO;
        }

        SysUser user = getCurrentUser(userId);
        UserVO userVO = convertToVO(user);
        cacheUser(userId, userVO);

        return userVO;
    }

    /**
     * 更新用户个人信息（含内容审核）
     * <p>审核流程：</p>
     * <ul>
     *   <li>昵称：进行文本安全审核</li>
     *   <li>头像：进行图片安全审核</li>
     *   <li>反馈内容：进行文本安全审核后保存到反馈表</li>
     * </ul>
     * @param userId 用户ID
     * @param updateInfo 更新信息参数
     */
    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateParam updateInfo) {
        SysUser user = this.getById(userId);
        if (user == null) throw new BusinessException(BizMsgEnum.USER_NOT_EXIST);

        if (StrUtil.isNotBlank(updateInfo.getNickname())) {
            AuditSuggestEnum nicknameAudit = contentAuditService.auditText(userId, user.getOpenid(), updateInfo.getNickname(), AuditSceneEnum.PROFILE);
            if (AuditSuggestEnum.BLOCK.equals(nicknameAudit)) {
                throw new BusinessException(BizMsgEnum.AUDIT_TEXT_BLOCKED);
            }
            user.setNickname(updateInfo.getNickname());
        }

        if (StrUtil.isNotBlank(updateInfo.getFileIds())) {
            List<String> fileIdList = Arrays.asList(updateInfo.getFileIds().replace("[", "").replace("]", "").replace("\"", "").split(","));
            AuditSuggestEnum imageAudit = contentAuditService.auditImages(userId, user.getOpenid(), fileIdList, AuditSceneEnum.PROFILE);
            if (AuditSuggestEnum.BLOCK.equals(imageAudit)) {
                throw new BusinessException(BizMsgEnum.AUDIT_IMAGE_BLOCK);
            }
            user.setFileIds(updateInfo.getFileIds());
        }

        this.updateById(user);

        if (StrUtil.isNotBlank(updateInfo.getFeedbackContent())) {
            userFeedbackService.submitFeedback(userId, updateInfo.getFeedbackContent());
        }

        clearUserCache(userId);
        log.debug("用户{}信息已更新，缓存已清除", userId);
    }

    @Override
    public UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setOpenid(user.getOpenid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setFileIds(user.getFileIds());
        vo.setAvatarUrl(resolveAvatarUrl(user.getFileIds()));
        return vo;
    }

    /**
     * 解析头像URL
     * @param fileIds 附件ID的JSON字符串
     * @return 头像完整URL，无头像时返回null
     */
    private String resolveAvatarUrl(String fileIds) {
        if (StrUtil.isBlank(fileIds)) {
            log.debug("解析头像URL: fileIds为空");
            return null;
        }
        try {
            String cleanIds = fileIds.replace("[", "").replace("]", "").replace("\"", "").trim();
            if (StrUtil.isBlank(cleanIds)) {
                log.debug("解析头像URL: 清理后fileIds为空");
                return null;
            }
            String firstId = cleanIds.split(",")[0].trim();
            if (StrUtil.isBlank(firstId)) {
                log.debug("解析头像URL: 第一个ID为空");
                return null;
            }
            Long id = Long.parseLong(firstId);
            log.debug("解析头像URL: fileIds={}, 解析出ID={}", fileIds, id);
            // 走 AttachmentService.getUrl：私有 Bucket 模式下会自动生成短期签名 URL
            String url = attachmentService.getUrl(id);
            if (url == null) {
                log.warn("解析头像URL: 附件不存在, id={}", id);
                return null;
            }
            log.debug("解析头像URL: 附件ID={}", id);
            return url;
        } catch (Exception e) {
            log.warn("解析头像URL失败: fileIds={}, error={}", fileIds, e.getMessage());
            return null;
        }
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
