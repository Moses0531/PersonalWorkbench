package com.moses.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.auth.entity.Register;
import com.moses.auth.entity.SysAuth;
import com.moses.config.ResultConfig;

/**
* @author Administrator
* @description 针对表【sys_account(系统账号表)】的数据库操作Service
* @createDate 2026-07-11 16:45:46
*/
public interface SysAuthService extends IService<SysAuth> {

    /**
     * 用户注册：手机号或邮箱二选一，账号由系统随机生成8位数
     */
    ResultConfig register(Register register);
}
