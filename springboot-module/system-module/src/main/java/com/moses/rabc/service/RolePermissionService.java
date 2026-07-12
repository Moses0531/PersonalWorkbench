package com.moses.rabc.service;

import com.moses.rabc.entity.RolePermission;
import com.moses.rabc.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Administrator
* @description 针对表【role_permission(角色-权限关联表)】的数据库操作Service
* @createDate 2026-07-12 12:04:43
*/
public interface RolePermissionService extends IService<RolePermission> {

    List<SysPermission> getPermissionsByRoleId(Long roleId);

    void addRolePermission(RolePermission rolePermission);

    void removeRolePermission(Long roleId, Long permissionId);

}
