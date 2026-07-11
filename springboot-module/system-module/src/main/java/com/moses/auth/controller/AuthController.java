package com.moses.auth.controller;

import com.moses.config.ResultConfig;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/* 用户注册与登录 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/register")
    public ResultConfig register() {

        return ResultConfig.success();
    }

    @PostMapping("/login")
    public ResultConfig login() {

        return ResultConfig.success();
    }
}
