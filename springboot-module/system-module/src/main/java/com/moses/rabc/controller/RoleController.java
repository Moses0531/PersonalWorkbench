package com.moses.rabc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.rabc.entity.SysRole;
import com.moses.rabc.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/role")
@Tag(name = "角色管理", description = "提供角色管理相关接口 (仅root可操作)")
public class RoleController {
    @Autowired
    private SysRoleService sysRoleService;

    @GetMapping("/getRolePage")
    @Operation(summary = "获取角色分页列表", description = "获取角色分页列表")
    @SaCheckPermission("role:query")
    public ResultConfig getRolePage(@RequestParam(value = "pageNum", defaultValue = "1") Integer current,
                                    @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows){
        IPage<SysRole> roleIPage = sysRoleService.page(new Page<>(current, pageRows));
        return ResultConfig.success(roleIPage);
    }

    @Operation(summary = "新增角色")
    @SaCheckPermission("role:add")
    @PostMapping("/addRole")
    public ResultConfig addRole(@RequestBody SysRole sysRole){
        sysRoleService.save(sysRole);
        return ResultConfig.success();
    }

    @Operation(summary = "修改角色")
    @SaCheckPermission("role:modify")
    @PostMapping("/updateRole")
    public ResultConfig updateRole(@RequestBody SysRole sysRole){
        sysRoleService.updateById(sysRole);
        return ResultConfig.success();
    }

    @Operation(summary = "删除角色(多个)")
    @SaCheckPermission("role:remove")
    @DeleteMapping("/deleteBatchRole")
    public ResultConfig deleteBatchRole(@RequestBody List<Integer> ids) {
        sysRoleService.removeByIds(ids);
        return ResultConfig.success();
    }
}
