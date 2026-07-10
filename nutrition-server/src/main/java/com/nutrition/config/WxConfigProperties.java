package com.nutrition.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性类
 * 通过系统环境变量读取 AppID 和 AppSecret
 * 环境变量名：WX_MINI_APPID、WX_MINI_APPSECRET
 */
@Data
@Component
@Slf4j
@ConfigurationProperties(prefix = "wx.mini")
public class WxConfigProperties {

    /**
     * 小程序 AppID
     * 通过环境变量 WX_MINI_APPID 注入
     */
    private String appId;

    /**
     * 小程序 AppSecret
     * 通过环境变量 WX_MINI_APPSECRET 注入
     */
    private String appSecret;

    /**
     * Token 有效期（秒），默认 7200 秒（2小时）
     */
    private int tokenExpireSeconds = 7200;

    /**
     * Token 提前刷新时间（秒），默认 60 秒
     */
    private int tokenRefreshSeconds = 60;

    /**
     * 初始化校验
     */
    @PostConstruct
    public void init() {
        if (appId == null || appId.isEmpty()) {
            log.warn("微信小程序 AppID 未配置，请设置环境变量 WX_MINI_APPID");
        } else {
            log.info("微信小程序 AppID 加载成功");
        }
        if (appSecret == null || appSecret.isEmpty()) {
            log.warn("微信小程序 AppSecret 未配置，请设置环境变量 WX_MINI_APPSECRET");
        } else {
            log.info("微信小程序 AppSecret 加载成功");
        }
    }

    /**
     * 检查配置是否完整
     */
    public boolean isConfigured() {
        return appId != null && !appId.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }
}
