package com.moses.rabc.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.rabc.entity.SysPermission;
import com.moses.rabc.service.SysPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
@Tag(name = "权限管理", description = "提供权限管理相关接口 (仅root可操作)")
public class SysPermissionController {
    @Autowired
    private SysPermissionService sysPermissionService;

    @GetMapping("/getPermissionPage")
    @Operation(summary = "获取权限分页列表", description = "获取权限分页列表")
    @SaCheckPermission("permission:query")
    public ResultConfig getPermissionPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer current,
                                    @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows){
        IPage<SysPermission> permissionIPage = sysPermissionService.page(new Page<>(current, pageRows));
        return ResultConfig.success(permissionIPage);
    }

    @Operation(summary = "新增权限")
    @SaCheckPermission("permission:add")
    @PostMapping("/addPermission")
    public ResultConfig addPermission(@RequestBody SysPermission sysPermission){
        sysPermissionService.save(sysPermission);
        return ResultConfig.success();
    }

    @Operation(summary = "修改权限")
    @SaCheckPermission("permission:modify")
    @PostMapping("/updatePermission")
    public ResultConfig updateRole(@RequestBody SysPermission sysPermission){
        sysPermissionService.updateById(sysPermission);
        return ResultConfig.success();
    }

    @Operation(summary = "批量删除权限")
    @SaCheckPermission("permission:remove")
    @DeleteMapping("/deleteBatchPermission")
    public ResultConfig deleteBatchPermission(@RequestBody List<Integer> ids) {
        sysPermissionService.removeByIds(ids);
        return ResultConfig.success();
    }
}
