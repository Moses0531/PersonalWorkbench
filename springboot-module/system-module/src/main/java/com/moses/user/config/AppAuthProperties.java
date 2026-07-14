package com.moses.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关功能开关（部署级配置）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {

    /** 是否允许用户自行注册 */
    private boolean registerEnabled = true;

    /** 登录是否需要图形验证码 */
    private boolean captchaOnLogin = true;

    /** 注册是否需要图形验证码 */
    private boolean captchaOnRegister = true;
}
