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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wbTask")
@Tag(name = "工作台-任务", description = "个人任务树相关接口")
public class WbTaskController {

    @Autowired
    private WbTaskService wbTaskService;

    @GetMapping("/getTaskPage")
    @Operation(summary = "获取任务分页列表", description = "仅返回当前登录用户的任务；可按 nodeType / parentTaskId / archived 过滤")
    @SaCheckPermission("task:query")
    public ResultConfig getTaskPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows,
                                    @RequestParam(value = "nodeType", required = false) String nodeType,
                                    @RequestParam(value = "parentTaskId", required = false) Long parentTaskId,
                                    @RequestParam(value = "archived", required = false) String archived) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<WbTask> qw = new LambdaQueryWrapper<WbTask>()
                .eq(WbTask::getUserId, userId);
        if (StringUtils.hasText(nodeType)) {
            qw.eq(WbTask::getNodeType, nodeType.trim());
        }
        if (parentTaskId != null) {
            qw.eq(WbTask::getParentTaskId, parentTaskId);
        }
        if (StringUtils.hasText(archived)) {
            qw.eq(WbTask::getArchived, archived.trim());
        }
        qw.orderByAsc(WbTask::getDisplayOrder).orderByDesc(WbTask::getUpdateTime);
        IPage<WbTask> page = wbTaskService.page(new Page<>(pageNum, pageRows), qw);
        return ResultConfig.success(page);
    }

    @PostMapping("/addTask")
    @Operation(summary = "新增任务/阶段/顶层任务")
    @SaCheckPermission("task:add")
    public ResultConfig addTask(@RequestBody WbTask wbTask) {
        wbTaskService.createOwnedTask(StpUtil.getLoginIdAsLong(), wbTask);
        return ResultConfig.success();
    }

    @PostMapping("/updateTask")
    @Operation(summary = "修改任务/阶段/顶层任务")
    @SaCheckPermission("task:modify")
    public ResultConfig updateTask(@RequestBody WbTask wbTask) {
        wbTaskService.updateOwnedTask(StpUtil.getLoginIdAsLong(), wbTask);
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchTask")
    @Operation(summary = "批量删除任务（级联删除子孙）")
    @SaCheckPermission("task:remove")
    public ResultConfig deleteBatchTask(@RequestBody List<Long> ids) {
        wbTaskService.deleteOwnedTasksCascade(StpUtil.getLoginIdAsLong(), ids);
        return ResultConfig.success();
    }

    @PostMapping("/setProjectArchived")
    @Operation(summary = "归档/恢复顶层任务")
    @SaCheckPermission("task:modify")
    public ResultConfig setProjectArchived(@RequestBody Map<String, Object> body) {
        Long projectTaskId = body.get("projectTaskId") != null
                ? Long.valueOf(String.valueOf(body.get("projectTaskId")))
                : (body.get("taskId") != null ? Long.valueOf(String.valueOf(body.get("taskId"))) : null);
        String archived = body.get("archived") != null ? String.valueOf(body.get("archived")) : null;
        wbTaskService.setProjectArchived(StpUtil.getLoginIdAsLong(), projectTaskId, archived);
        return ResultConfig.success();
    }

    @PostMapping("/uploadAttachment")
    @Operation(summary = "上传任务附件（阿里云 OSS）")
    @SaCheckPermission("task:modify")
    public ResultConfig uploadAttachment(@RequestParam("taskId") Long taskId,
                                         @RequestParam("file") MultipartFile file) {
        return ResultConfig.success(
                wbTaskService.uploadAttachment(StpUtil.getLoginIdAsLong(), taskId, file)
        );
    }

    @DeleteMapping("/removeAttachment")
    @Operation(summary = "删除任务附件")
    @SaCheckPermission("task:modify")
    public ResultConfig removeAttachment(@RequestBody Map<String, Object> body) {
        Long taskId = body.get("taskId") != null ? Long.valueOf(String.valueOf(body.get("taskId"))) : null;
        String attachmentId = body.get("attachmentId") != null ? String.valueOf(body.get("attachmentId")) : null;
        wbTaskService.removeAttachment(StpUtil.getLoginIdAsLong(), taskId, attachmentId);
        return ResultConfig.success();
    }

    @PostMapping("/updateAttachmentStatus")
    @Operation(summary = "更新任务附件处理状态（0-未处理，1-已处理，2-处理完成）")
    @SaCheckPermission("task:modify")
    public ResultConfig updateAttachmentStatus(@RequestBody Map<String, Object> body) {
        Long taskId = body.get("taskId") != null ? Long.valueOf(String.valueOf(body.get("taskId"))) : null;
        String attachmentId = body.get("attachmentId") != null ? String.valueOf(body.get("attachmentId")) : null;
        String status = body.get("status") != null ? String.valueOf(body.get("status")) : null;
        return ResultConfig.success(
                wbTaskService.updateAttachmentStatus(
                        StpUtil.getLoginIdAsLong(), taskId, attachmentId, status)
        );
    }

    @GetMapping("/listProjectAttachments")
    @Operation(summary = "任务组附件审查（汇总子孙任务附件）")
    @SaCheckPermission("task:query")
    public ResultConfig listProjectAttachments(
            @RequestParam(value = "projectTaskId", required = false) Long projectTaskId,
            @RequestParam(value = "projectId", required = false) Long projectId) {
        Long id = projectTaskId != null ? projectTaskId : projectId;
        return ResultConfig.success(
                wbTaskService.listProjectAttachments(StpUtil.getLoginIdAsLong(), id)
        );
    }
}
