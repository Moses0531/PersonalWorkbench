package com.moses.rabc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@Tag(name = "用户/管理员列表", description = "查看用户列表/系统管理员信息管理")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/getPageUser")
    @Operation(summary = "分页查询普通用户列表")
    @SaCheckPermission("user:query")
    public ResultConfig getPageUser(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        IPage<SysUser> userPage = sysUserService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 2L)
        );
        userPage.getRecords().forEach(user -> user.setPassword(null));
        return ResultConfig.success(userPage);
    }

    @GetMapping("/getPageAdmin")
    @Operation(summary = "分页查询管理员列表")
    @SaCheckPermission("admin:query")
    public ResultConfig getPageAdmin(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                     @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        IPage<SysUser> adminPage = sysUserService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 1L)
        );
        adminPage.getRecords().forEach(user -> user.setPassword(null));
        return ResultConfig.success(adminPage);
    }

    @Operation(summary = "新增用户")
    @SaCheckPermission(value = {"user:add", "admin:add"}, mode = SaMode.OR)
    @PostMapping("/insertUser")
    public ResultConfig addUser(@RequestBody SysUser user) {
        sysUserService.save(user);
        return ResultConfig.success();
    }

    @Operation(summary = "更新用户")
    @SaCheckPermission(value = {"user:modify", "admin:modify"}, mode = SaMode.OR)
    @PostMapping("/updateUser")
    public ResultConfig updateUser(@RequestBody SysUser user) {
        sysUserService.updateById(user);
        return ResultConfig.success();
    }

    @Operation(summary = "批量删除用户")
    @SaCheckPermission(value = {"user:remove", "admin:remove"}, mode = SaMode.OR)
    @DeleteMapping("/deleteBatchUser")
    public ResultConfig deleteBatchUser(@RequestBody List<Long> ids) {
        sysUserService.removeByIds(ids);
        return ResultConfig.success();
    }
}
