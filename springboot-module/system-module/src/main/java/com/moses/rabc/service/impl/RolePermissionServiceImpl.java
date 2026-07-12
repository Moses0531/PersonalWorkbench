package com.moses.rabc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.rabc.entity.RolePermission;
import com.moses.rabc.service.RolePermissionService;
import com.moses.rabc.mapper.RolePermissionMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【role_permission(角色-权限关联表)】的数据库操作Service实现
* @createDate 2026-07-12 12:04:43
*/
@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>
    implements RolePermissionService{

}




