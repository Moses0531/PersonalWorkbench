package com.moses.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.moses.config.ResultConfig;
import com.moses.user.entity.SysUser;
import com.moses.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户个人信息修改", description = "提供用户个人信息修改接口")
@RestController
@RequestMapping("/user-profile")
public class UserProfileController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "获取当前用户信息")
    @SaCheckLogin
    @GetMapping("/getCurrentUserProfile")
    public ResultConfig getCurrentUserInfo() {
        SysUser userProfile = sysUserService.getCurrentUserInfo(StpUtil.getLoginIdAsLong());
        return ResultConfig.success(userProfile);
    }

    @Operation(summary = "更新个人信息")
    @SaCheckLogin
    @SaCheckPermission("profile:modify")
    @PutMapping("/updateUserProfile")
    public ResultConfig updateUserInfo(@RequestBody SysUser userProfile) {
        sysUserService.updateUserInfo(StpUtil.getLoginIdAsLong(), userProfile);
        return ResultConfig.success("个人信息更新成功");
    }

    @Operation(summary = "修改密码")
    @SaCheckLogin
    @SaCheckPermission("profile:modify")
    @PostMapping("/changePassword")
    public ResultConfig changePassword(@RequestBody Map<String, String> passwordData) {
        sysUserService.changePassword(
                StpUtil.getLoginIdAsLong(),
                passwordData.get("oldPassword"),
                passwordData.get("newPassword")
        );
        return ResultConfig.success("密码修改成功");
    }

    @Operation(summary = "上传用户头像")
    @SaCheckLogin
    @SaCheckPermission("profile:modify")
    @PostMapping("/updateAvatar")
    public ResultConfig updateAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = sysUserService.updateAvatar(StpUtil.getLoginIdAsLong(), file);
        return ResultConfig.success(avatarUrl);
    }
}
