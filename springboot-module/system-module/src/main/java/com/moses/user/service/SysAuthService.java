package com.moses.user.service;

import com.moses.user.entity.Login;
import com.moses.user.entity.Register;

/**
 * 用户认证服务
 */
public interface SysAuthService {

    Register register(Register register);

    Login login(Login login);
}
