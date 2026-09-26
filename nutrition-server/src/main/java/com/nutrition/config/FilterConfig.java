package com.nutrition.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器注册配置
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthFilter);
        // 除业务接口外，把 Swagger / OpenAPI 文档路径也纳入过滤器，
        // 即使通过 SWAGGER_ENABLED=true 打开，也必须携带 ADMIN 令牌才能访问
        registration.addUrlPatterns(
                "/api/*",
                "/v3/api-docs",
                "/v3/api-docs/*",
                "/swagger-ui/*",
                "/swagger-ui.html");
        registration.setOrder(1);
        return registration;
    }
}
