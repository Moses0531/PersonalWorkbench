package com.moses.rabc.mapper;

import com.moses.rabc.entity.RolePermission;
import com.moses.rabc.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Administrator
* @description 针对表【role_permission(角色-权限关联表)】的数据库操作Mapper
* @createDate 2026-07-12 12:04:43
* @Entity com.moses.rabc.entity.RolePermission
*/
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    List<SysPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    int deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

}




