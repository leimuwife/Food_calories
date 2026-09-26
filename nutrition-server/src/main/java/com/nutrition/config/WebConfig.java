package com.nutrition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web 配置 - 跨域 & 密码编码器 & 全局路径前缀
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 允许跨域访问的来源白名单，逗号分隔。
     * 默认空：同域部署（Nginx 反代 /api）下无需 CORS，不注册任何跨域规则。
     * 若管理后台独立子域名，可配置为 https://admin.example.com。
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> true);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 不再使用 allowedOriginPatterns("*")，避免任意站点携带凭据跨域调用接口
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            return;
        }
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        if (origins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
