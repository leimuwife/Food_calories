package com.nutrition.user;

import com.nutrition.common.BusinessException;
import com.nutrition.entity.SysUser;
import com.nutrition.enums.BizMsgEnum;
import com.nutrition.mapper.SysUserMapper;
import com.nutrition.param.RegisterParam;
import com.nutrition.param.ResetPasswordParam;
import com.nutrition.service.AttachmentService;
import com.nutrition.service.CaptchaService;
import com.nutrition.service.ContentAuditService;
import com.nutrition.service.UserFeedbackService;
import com.nutrition.service.impl.UserServiceImpl;
import com.nutrition.util.JwtUtil;
import com.nutrition.util.RedisCache;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册（手机号）与重置密码功能测试。
 * <p>
 * 覆盖：手机号格式校验、注册写入手机号、重置密码时用户名+手机号匹配校验。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class UserAccountTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisCache redisCache;
    @Mock
    private ContentAuditService contentAuditService;
    @Mock
    private UserFeedbackService userFeedbackService;
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private CaptchaService captchaService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserServiceImpl userService;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                passwordEncoder, jwtUtil, redisCache,
                contentAuditService, userFeedbackService, attachmentService, captchaService);
        // ServiceImpl 的 baseMapper 为 protected 字段，测试中通过反射注入 mock
        ReflectionTestUtils.setField(userService, "baseMapper", sysUserMapper);
    }

    // ==================== 手机号格式校验 ====================

    /** 合法手机号应通过注册参数校验。 */
    @Test
    void shouldAcceptValidPhoneOnRegister() {
        RegisterParam param = baseRegisterParam();
        param.setPhone("13800138000");
        assertTrue(validator.validate(param).isEmpty(), "合法手机号不应产生校验错误");
    }

    /** 位数不足、位数超长、非法号段均应被拒绝。 */
    @Test
    void shouldRejectInvalidPhoneOnRegister() {
        for (String badPhone : new String[]{"1380013800", "138001380000", "12345678901", "23800138000", "1380013800a"}) {
            RegisterParam param = baseRegisterParam();
            param.setPhone(badPhone);
            Set<ConstraintViolation<RegisterParam>> violations = validator.validate(param);
            assertFalse(violations.isEmpty(), "非法手机号应被拒绝: " + badPhone);
        }
    }

    /** 重置密码参数同样校验手机号格式。 */
    @Test
    void shouldRejectInvalidPhoneOnReset() {
        ResetPasswordParam param = new ResetPasswordParam();
        param.setUsername("alice");
        param.setPhone("1380013800");
        param.setNewPassword("newpass123");
        param.setConfirmPassword("newpass123");
        assertFalse(validator.validate(param).isEmpty());
    }

    // ==================== 注册写入手机号 ====================

    /** 注册成功后应写入手机号，且不再写入任何可逆密码字段。 */
    @Test
    void shouldPersistPhoneOnRegister() {
        RegisterParam param = baseRegisterParam();
        param.setPhone("13900139000");

        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        userService.register(param);

        verify(sysUserMapper).insert(any(SysUser.class));
        // 注册时校验了图形验证码
        verify(captchaService).validateCaptcha(param.getCaptchaId(), param.getCaptchaCode());
    }

    // ==================== 重置密码 ====================

    /** 用户名 + 手机号匹配时重置成功，并写入新的 BCrypt 哈希。 */
    @Test
    void shouldResetPasswordWhenPhoneMatches() {
        SysUser user = existingUser("alice", "13800138000");
        stubUserLookup(user);

        ResetPasswordParam param = resetParam("alice", "13800138000", "newpass123");
        userService.resetPassword(param);

        verify(sysUserMapper).updateById(any(SysUser.class));
    }

    /** 手机号不匹配时必须提示「手机号错误」，且不得更新密码。 */
    @Test
    void shouldRejectResetWhenPhoneMismatch() {
        SysUser user = existingUser("alice", "13800138000");
        stubUserLookup(user);

        ResetPasswordParam param = resetParam("alice", "13900139000", "newpass123");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.resetPassword(param));
        assertEquals(BizMsgEnum.PHONE_NOT_MATCH.getMessage(), ex.getMessage());
    }

    /** 用户不存在时同样提示「手机号错误」，避免用户名枚举。 */
    @Test
    void shouldRejectResetWhenUserNotFound() {
        stubUserLookup(null);

        ResetPasswordParam param = resetParam("nobody", "13800138000", "newpass123");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.resetPassword(param));
        assertEquals(BizMsgEnum.PHONE_NOT_MATCH.getMessage(), ex.getMessage());
    }

    /** 两次新密码不一致时拒绝重置。 */
    @Test
    void shouldRejectResetWhenConfirmMismatch() {
        ResetPasswordParam param = resetParam("alice", "13800138000", "newpass123");
        param.setConfirmPassword("different123");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.resetPassword(param));
        assertEquals(BizMsgEnum.PASSWORD_NOT_MATCH.getMessage(), ex.getMessage());
    }

    // ==================== 辅助方法 ====================

    private RegisterParam baseRegisterParam() {
        RegisterParam param = new RegisterParam();
        param.setUsername("alice");
        param.setPassword("pass123456");
        param.setConfirmPassword("pass123456");
        param.setCaptchaId("captcha-id");
        param.setCaptchaCode("1234");
        param.setPhone("13800138000");
        return param;
    }

    private ResetPasswordParam resetParam(String username, String phone, String newPassword) {
        ResetPasswordParam param = new ResetPasswordParam();
        param.setUsername(username);
        param.setPhone(phone);
        param.setNewPassword(newPassword);
        param.setConfirmPassword(newPassword);
        return param;
    }

    private SysUser existingUser(String username, String phone) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("oldpass123"));
        return user;
    }

    /**
     * MyBatis-Plus 3.5.7 中 ServiceImpl#getOne 实际调用
     * {@code baseMapper.selectOne(wrapper, throwEx)}，此处按真实调用链打桩。
     */
    private void stubUserLookup(SysUser user) {
        lenient().when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(user);
    }
}
