package com.moses.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.auth.entity.Login;
import com.moses.auth.entity.Register;
import com.moses.auth.entity.SysAuth;

/**
 * @author Administrator
 * @description 针对表【sys_auth(系统账号表)】的数据库操作Service
 * @createDate 2026-07-11 16:45:46
 */
public interface SysAuthService extends IService<SysAuth> {

    Register register(Register register);

    Login login(Login login);
}
