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
     * 微信审核接口版本
     * 取值：1 或 2，默认值为 1
     * v1：旧版接口，openid 非必填，兼容测试占位 openid
     * v2：新版接口，openid 必填，启用高精度风控模型与详细违规标签
     */
    private int auditVersion = 1;

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

        if (auditVersion != 1 && auditVersion != 2) {
            log.warn("微信审核接口版本配置值 {} 无效，强制重置为 1", auditVersion);
            this.auditVersion = 1;
        }
        log.info("微信审核接口版本配置: v{}", auditVersion);
    }

    /**
     * 检查配置是否完整
     */
    public boolean isConfigured() {
        return appId != null && !appId.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }

    /**
     * 判断是否使用 v2 版本审核接口
     *
     * @return true 使用 v2 版本，false 使用 v1 版本
     */
    public boolean isAuditVersionV2() {
        return auditVersion == 2;
    }
}
