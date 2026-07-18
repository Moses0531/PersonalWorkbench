package com.moses.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/captchas/**",
                        "/auth/login",
                        "/auth/register",
                        "/auth/config",
                        "/doc.html",
                        "/favicon.ico",
                        "/webjars/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/error"
                );
    }
}

/**
 @SaCheckLogin: 登录校验 —— 只有登录之后才能进入该方法。
 @SaCheckRole("admin"): 角色校验 —— 必须具有指定角色标识才能进入该方法。
 @SaCheckPermission("user:add"): 权限校验 —— 必须具有指定权限才能进入该方法。
 @SaCheckSafe: 二级认证校验 —— 必须二级认证之后才能进入该方法。
 @SaCheckHttpBasic: HttpBasic校 验 —— 只有通过 HttpBasic 认证后才能进入该方法。
 @SaCheckHttpDigest: HttpDigest校验 —— 只有通过 HttpDigest 认证后才能进入该方法。
 @SaCheckDisable("comment")：账号服务封禁校验 —— 校验当前账号指定服务是否被封禁。
 @SaCheckSign：API 签名校验 —— 用于跨系统的 API 签名参数校验。
 @SaIgnore：忽略校验 —— 表示被修饰的方法或类无需进行注解鉴权和路由拦截器鉴权。
 */
