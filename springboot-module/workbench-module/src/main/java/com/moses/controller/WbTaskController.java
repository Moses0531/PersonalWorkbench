package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbTask;
import com.moses.service.WbTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wbTask")
@Tag(name = "工作台-任务", description = "个人任务相关接口")
public class WbTaskController {

    @Autowired
    private WbTaskService wbTaskService;

    @GetMapping("/getTaskPage")
    @Operation(summary = "获取任务分页列表", description = "仅返回当前登录用户的任务")
    @SaCheckPermission("task:query")
    public ResultConfig getTaskPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<WbTask> page = wbTaskService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getUserId, userId)
                        .orderByAsc(WbTask::getDisplayOrder)
                        .orderByDesc(WbTask::getUpdateTime)
        );
        return ResultConfig.success(page);
    }

    @PostMapping("/addTask")
    @Operation(summary = "新增任务")
    @SaCheckPermission("task:add")
    public ResultConfig addTask(@RequestBody WbTask wbTask) {
        wbTask.setTaskId(null);
        wbTask.setUserId(StpUtil.getLoginIdAsLong());
        wbTaskService.save(wbTask);
        return ResultConfig.success();
    }

    @PostMapping("/updateTask")
    @Operation(summary = "修改任务")
    @SaCheckPermission("task:modify")
    public ResultConfig updateTask(@RequestBody WbTask wbTask) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbTask.setUserId(userId);
        wbTaskService.update(
                wbTask,
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getTaskId, wbTask.getTaskId())
                        .eq(WbTask::getUserId, userId)
        );
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchTask")
    @Operation(summary = "批量删除任务")
    @SaCheckPermission("task:remove")
    public ResultConfig deleteBatchTask(@RequestBody List<Long> ids) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbTaskService.remove(
                new LambdaQueryWrapper<WbTask>()
                        .in(WbTask::getTaskId, ids)
                        .eq(WbTask::getUserId, userId)
        );
        return ResultConfig.success();
    }
}
