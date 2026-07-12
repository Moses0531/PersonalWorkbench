package com.moses.security;

import cn.dev33.satoken.stp.StpInterface;
import com.moses.rabc.mapper.SysPermissionMapper;
import com.moses.rabc.mapper.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限加载接口实现类
 */
@Component
public class StpInterfaceConfig implements StpInterface {

    @Autowired
    private  SysPermissionMapper sysPermissionMapper;
    @Autowired
    private  SysRoleMapper sysRoleMapper;

    public StpInterfaceConfig(SysPermissionMapper sysPermissionMapper, SysRoleMapper sysRoleMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        List<String> permissionList = sysPermissionMapper.selectPermissionCodesByUserId(userId);
        if (permissionList == null) {
            return new ArrayList<>();
        }
        return permissionList;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        String roleCode = sysRoleMapper.selectRoleCodeByUserId(userId);
        List<String> roleList = new ArrayList<>();
        if (roleCode != null && !roleCode.trim().isEmpty()) {
            roleList.add(roleCode);
        }
        return roleList;
    }
}
