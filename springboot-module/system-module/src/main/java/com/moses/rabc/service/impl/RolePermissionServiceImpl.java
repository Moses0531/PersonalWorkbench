package com.moses.rabc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.rabc.entity.RolePermission;
import com.moses.rabc.entity.SysPermission;
import com.moses.rabc.entity.SysRole;
import com.moses.rabc.mapper.RolePermissionMapper;
import com.moses.rabc.service.RolePermissionService;
import com.moses.rabc.service.SysPermissionService;
import com.moses.rabc.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Administrator
* @description 针对表【role_permission(角色-权限关联表)】的数据库操作Service实现
* @createDate 2026-07-12 12:04:43
*/
@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>
    implements RolePermissionService{

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysPermissionService sysPermissionService;

    @Override
    public List<SysPermission> getPermissionsByRoleId(Long roleId) {
        requireRole(roleId);
        return baseMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public void addRolePermission(RolePermission rolePermission) {
        if (rolePermission == null || rolePermission.getRoleId() == null || rolePermission.getPermissionId() == null) {
            throw new RuntimeException("角色ID和权限ID不能为空");
        }
        requireRole(rolePermission.getRoleId());
        if (sysPermissionService.getById(rolePermission.getPermissionId()) == null) {
            throw new RuntimeException("权限不存在");
        }
        long count = count(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, rolePermission.getRoleId())
                .eq(RolePermission::getPermissionId, rolePermission.getPermissionId()));
        if (count > 0) {
            throw new RuntimeException("该角色已拥有此权限");
        }
        save(rolePermission);
    }

    @Override
    public void removeRolePermission(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            throw new RuntimeException("角色ID和权限ID不能为空");
        }
        int rows = baseMapper.deleteByRoleIdAndPermissionId(roleId, permissionId);
        if (rows == 0) {
            throw new RuntimeException("角色权限关联不存在");
        }
    }

    /** role_id=0 时 getById 可能查不到，需显式按主键查询 */
    private SysRole requireRole(Long roleId) {
        if (roleId == null) {
            throw new RuntimeException("角色ID不能为空");
        }
        SysRole role = sysRoleService.getById(roleId);
        if (role == null) {
            role = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleId, roleId), false);
        }
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return role;
    }

}



