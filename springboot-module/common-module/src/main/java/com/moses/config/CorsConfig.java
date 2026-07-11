package com.moses.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173") // 允许来自前端Vite开发服务器的请求
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许这些HTTP方法，包括OPTIONS
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许携带身份凭证
                .maxAge(3600); // 预检请求缓存时间
    }
}