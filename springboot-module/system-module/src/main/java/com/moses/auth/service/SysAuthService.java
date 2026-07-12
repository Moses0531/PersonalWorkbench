package com.moses.auth.service;

import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;

/**
 * 用户认证服务
 */
public interface SysAuthService {

    Register register(Register register);

    Login login(Login login);
}
