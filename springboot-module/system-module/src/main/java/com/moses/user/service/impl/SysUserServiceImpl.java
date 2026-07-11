package com.moses.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import com.moses.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【sys_user(用户信息表)】的数据库操作Service实现
* @createDate 2026-07-11 17:42:14
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

}




