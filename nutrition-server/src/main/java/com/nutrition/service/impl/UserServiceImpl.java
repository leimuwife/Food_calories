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
import com.nutrition.entity.Attachment;
import com.nutrition.entity.SysUser;
import com.nutrition.mapper.SysUserMapper;
import com.nutrition.service.AttachmentService;
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

    @Override
    public LoginResultVO login(LoginParam param) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, param.getUsername()));
        if (user == null) {
            throw new BusinessException(BizMsgEnum.USER_LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(param.getPassword(), user.getPasswordHash())) {
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

    @Override
    @Transactional
    public LoginResultVO register(RegisterParam param) {
        long count = this.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, param.getUsername()));
        if (count > 0) {
            throw new BusinessException(BizMsgEnum.USER_NAME_EXIST);
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
                yield user;
            }
            case "test_code_user2" -> {
                user.setNickname("李四");
                user.setUsername("lisi");
                yield user;
            }
            case "test_code_nutritionist" -> {
                user.setNickname("小张营养师");
                user.setUsername("nutritionist_zhang");
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
            Attachment attachment = attachmentService.getById(id);
            if (attachment == null) {
                log.warn("解析头像URL: 附件不存在, id={}", id);
                return null;
            }
            String url = attachment.getFileUrl();
            log.debug("解析头像URL: 附件ID={}, fileUrl={}", id, url);
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