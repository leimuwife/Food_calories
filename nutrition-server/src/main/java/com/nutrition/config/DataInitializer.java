package com.nutrition.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nutrition.entity.SysUser;
import com.nutrition.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 * 应用启动时初始化测试数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initTestUser();
        log.info("数据初始化完成");
    }

    private void initTestUser() {
        SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "test"));
        
        if (existing == null) {
            SysUser testUser = new SysUser();
            testUser.setUsername("test");
            testUser.setPasswordHash(passwordEncoder.encode("123456"));
            testUser.setNickname("测试用户");
            testUser.setCreateTime(LocalDateTime.now());
            testUser.setUpdateTime(LocalDateTime.now());
            
            sysUserMapper.insert(testUser);
            log.info("创建测试用户: username=test, password=123456");
        }
    }
}
