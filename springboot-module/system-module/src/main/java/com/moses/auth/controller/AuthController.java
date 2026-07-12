package com.moses.auth.controller;

import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;
import com.moses.auth.service.SysAuthService;
import com.moses.config.ResultConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证管理 — 注册与登录接口
 */
@Tag(name = "用户认证", description = "提供用户注册、登录接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysAuthService sysAuthService;

    @Operation(summary = "用户注册", description = "通过手机号或邮箱进行注册，需要提供密码、确认密码及验证码")
    @PostMapping("/register")
    public ResultConfig userRegister(@RequestBody Register register) {
        return ResultConfig.success(sysAuthService.register(register));
    }

    @Operation(summary = "用户登录", description = "使用账号、手机号或邮箱登录，需要提供密码及验证码")
    @PostMapping("/login")
    public ResultConfig userLogin(@RequestBody Login login) {
        return ResultConfig.success(sysAuthService.login(login));
    }
}
