package com.moses.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.auth.entity.SysAuth;
import com.moses.auth.service.SysAuthService;
import com.moses.auth.mapper.SysAuthMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【sys_account(系统账号表)】的数据库操作Service实现
* @createDate 2026-07-11 16:45:46
*/
@Service
public class SysAuthServiceImpl extends ServiceImpl<SysAuthMapper, SysAuth>
    implements SysAuthService {

}



