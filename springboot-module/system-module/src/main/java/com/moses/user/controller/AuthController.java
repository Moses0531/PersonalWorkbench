package com.moses.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.moses.config.ResultConfig;
import com.moses.user.config.AppAuthProperties;
import com.moses.user.entity.Login;
import com.moses.user.entity.Register;
import com.moses.user.service.SysAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户认证管理 — 注册与登录接口
 */
@Tag(name = "用户认证", description = "提供用户注册、登录接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysAuthService sysAuthService;

    @Autowired
    private AppAuthProperties appAuthProperties;

    @Operation(summary = "获取认证公开配置", description = "返回注册与验证码开关，供登录页按需展示")
    @GetMapping("/config")
    public ResultConfig getAuthConfig() {
        Map<String, Object> config = new LinkedHashMap<>(3);
        config.put("registerEnabled", appAuthProperties.isRegisterEnabled());
        config.put("captchaOnLogin", appAuthProperties.isCaptchaOnLogin());
        config.put("captchaOnRegister", appAuthProperties.isCaptchaOnRegister());
        return ResultConfig.success(config);
    }

    @Operation(summary = "用户注册", description = "通过手机号或邮箱进行注册，需要提供密码、确认密码；验证码按配置可选")
    @PostMapping("/register")
    public ResultConfig userRegister(@RequestBody Register register) {
        return ResultConfig.success(sysAuthService.register(register));
    }

    @Operation(summary = "用户登录", description = "使用账号、手机号或邮箱登录，需要提供密码；验证码按配置可选")
    @PostMapping("/login")
    public ResultConfig userLogin(@RequestBody Login login) {
        return ResultConfig.success(sysAuthService.login(login));
    }

    @Operation(summary = "获取当前用户菜单", description = "从数据库实时加载当前登录用户可访问的菜单与功能权限")
    @SaCheckLogin
    @GetMapping("/menus")
    public ResultConfig getCurrentUserMenus() {
        return ResultConfig.success(sysAuthService.getCurrentUserMenus());
    }
}
