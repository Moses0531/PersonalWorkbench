package com.moses.auth.controller;

import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;
import com.moses.auth.service.SysAuthService;
import com.moses.config.ResultConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/* 用户注册与登录 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private  SysAuthService sysAuthService;



    @PostMapping("/register")
    public ResultConfig userRegister(@RequestBody Register register) {
        return ResultConfig.success(sysAuthService.register(register));
    }

    @PostMapping("/login")
    public ResultConfig userLogin(@RequestBody Login login) {
        return ResultConfig.success(sysAuthService.login(login));
    }
}
