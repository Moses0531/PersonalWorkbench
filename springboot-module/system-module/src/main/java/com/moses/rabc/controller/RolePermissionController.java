package com.moses.rabc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.moses.config.ResultConfig;
import com.moses.rabc.entity.RolePermission;
import com.moses.rabc.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rolePermission")
@Tag(name = "角色权限管理", description = "提供角色与权限关联相关接口")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping("/getPermissionsByRoleId")
    @Operation(summary = "查询角色对应的权限", description = "根据角色ID查询已绑定的权限列表")
    @SaCheckPermission("rolePermission:query")
    public ResultConfig getPermissionsByRoleId(@RequestParam Long roleId) {
        return ResultConfig.success(rolePermissionService.getPermissionsByRoleId(roleId));
    }

    @PostMapping("/addRolePermission")
    @Operation(summary = "给角色新增权限", description = "为指定角色绑定一个权限")
    @SaCheckPermission("rolePermission:add")
    public ResultConfig addRolePermission(@RequestBody RolePermission rolePermission) {
        rolePermissionService.addRolePermission(rolePermission);
        return ResultConfig.success();
    }

    @DeleteMapping("/removeRolePermission")
    @Operation(summary = "移除角色权限", description = "解除角色与指定权限的绑定")
    @SaCheckPermission("rolePermission:remove")
    public ResultConfig removeRolePermission(@RequestBody RolePermission rolePermission) {
        rolePermissionService.removeRolePermission(rolePermission.getRoleId(), rolePermission.getPermissionId());
        return ResultConfig.success();
    }
}
