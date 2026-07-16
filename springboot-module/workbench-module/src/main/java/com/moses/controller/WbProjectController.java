package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbProject;
import com.moses.service.WbProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wbProject")
@Tag(name = "工作台-项目", description = "个人项目相关接口")
public class WbProjectController {

    @Autowired
    private WbProjectService wbProjectService;

    @GetMapping("/getProjectPage")
    @Operation(summary = "获取项目分页列表", description = "仅返回当前登录用户的项目")
    @SaCheckPermission("project:query")
    public ResultConfig getProjectPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<WbProject> page = wbProjectService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<WbProject>()
                        .eq(WbProject::getUserId, userId)
                        .orderByAsc(WbProject::getDisplayOrder)
                        .orderByDesc(WbProject::getUpdateTime)
        );
        return ResultConfig.success(page);
    }

    @PostMapping("/addProject")
    @Operation(summary = "新增项目")
    @SaCheckPermission("project:add")
    public ResultConfig addProject(@RequestBody WbProject wbProject) {
        wbProject.setProjectId(null);
        wbProject.setUserId(StpUtil.getLoginIdAsLong());
        wbProjectService.save(wbProject);
        return ResultConfig.success();
    }

    @PostMapping("/updateProject")
    @Operation(summary = "修改项目")
    @SaCheckPermission("project:modify")
    public ResultConfig updateProject(@RequestBody WbProject wbProject) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbProject.setUserId(userId);
        wbProjectService.update(
                wbProject,
                new LambdaQueryWrapper<WbProject>()
                        .eq(WbProject::getProjectId, wbProject.getProjectId())
                        .eq(WbProject::getUserId, userId)
        );
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchProject")
    @Operation(summary = "批量删除项目")
    @SaCheckPermission("project:remove")
    public ResultConfig deleteBatchProject(@RequestBody List<Long> ids) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbProjectService.remove(
                new LambdaQueryWrapper<WbProject>()
                        .in(WbProject::getProjectId, ids)
                        .eq(WbProject::getUserId, userId)
        );
        return ResultConfig.success();
    }
}
