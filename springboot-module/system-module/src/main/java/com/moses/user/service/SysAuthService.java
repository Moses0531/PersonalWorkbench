package com.moses.user.service;

import com.moses.rabc.entity.SysPermission;
import com.moses.user.entity.Login;
import com.moses.user.entity.Register;

import java.util.List;

/**
 * 用户认证服务
 */
public interface SysAuthService {

    Register register(Register register);

    Login login(Login login);

    /** 按当前登录用户从数据库实时加载菜单（含父级目录补全） */
    List<SysPermission> getCurrentUserMenus();
}
