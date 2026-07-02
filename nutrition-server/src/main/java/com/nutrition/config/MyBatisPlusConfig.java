package com.nutrition.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 */
@Configuration
@MapperScan("com.nutrition.mapper")
public class MyBatisPlusConfig {
    // MyBatis-Plus 3.5.7+ 使用自动配置，无需额外 Bean
}
