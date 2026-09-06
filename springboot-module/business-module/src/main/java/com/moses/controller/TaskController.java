package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.Task;
import com.moses.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task")
@Tag(name = "业务-任务", description = "个人任务树相关接口")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/getTaskPage")
    @Operation(summary = "获取任务分页列表", description = "仅返回当前登录用户的任务；可按 nodeType / parentTaskId / archived 过滤")
    @SaCheckPermission("task:query")
    public ResultConfig getTaskPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows,
                                    @RequestParam(value = "nodeType", required = false) String nodeType,
                                    @RequestParam(value = "parentTaskId", required = false) Long parentTaskId,
                                    @RequestParam(value = "archived", required = false) String archived) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Task> qw = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId);
        if (StringUtils.hasText(nodeType)) {
            qw.eq(Task::getNodeType, nodeType.trim());
        }
        if (parentTaskId != null) {
            qw.eq(Task::getParentTaskId, parentTaskId);
        }
        if (StringUtils.hasText(archived)) {
            qw.eq(Task::getArchived, archived.trim());
        }
        qw.orderByAsc(Task::getDisplayOrder).orderByDesc(Task::getUpdateTime);
        IPage<Task> page = taskService.page(new Page<>(pageNum, pageRows), qw);
        return ResultConfig.success(page);
    }

    @PostMapping("/addTask")
    @Operation(summary = "新增任务/阶段/顶层任务")
    @SaCheckPermission("task:add")
    public ResultConfig addTask(@RequestBody Task task) {
        taskService.createOwnedTask(StpUtil.getLoginIdAsLong(), task);
        return ResultConfig.success();
    }

    @PostMapping("/updateTask")
    @Operation(summary = "修改任务/阶段/顶层任务")
    @SaCheckPermission("task:modify")
    public ResultConfig updateTask(@RequestBody Task task) {
        taskService.updateOwnedTask(StpUtil.getLoginIdAsLong(), task);
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchTask")
    @Operation(summary = "批量删除任务（级联删除子孙）")
    @SaCheckPermission("task:remove")
    public ResultConfig deleteBatchTask(@RequestBody List<Long> ids) {
        taskService.deleteOwnedTasksCascade(StpUtil.getLoginIdAsLong(), ids);
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
        taskService.setProjectArchived(StpUtil.getLoginIdAsLong(), projectTaskId, archived);
        return ResultConfig.success();
    }

    @PostMapping("/uploadAttachment")
    @Operation(summary = "上传任务附件（阿里云 OSS）")
    @SaCheckPermission("task:modify")
    public ResultConfig uploadAttachment(@RequestParam("taskId") Long taskId,
                                         @RequestParam("file") MultipartFile file) {
        return ResultConfig.success(
                taskService.uploadAttachment(StpUtil.getLoginIdAsLong(), taskId, file)
        );
    }

    @DeleteMapping("/removeAttachment")
    @Operation(summary = "删除任务附件")
    @SaCheckPermission("task:modify")
    public ResultConfig removeAttachment(@RequestBody Map<String, Object> body) {
        Long taskId = body.get("taskId") != null ? Long.valueOf(String.valueOf(body.get("taskId"))) : null;
        String attachmentId = body.get("attachmentId") != null ? String.valueOf(body.get("attachmentId")) : null;
        taskService.removeAttachment(StpUtil.getLoginIdAsLong(), taskId, attachmentId);
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
                taskService.updateAttachmentStatus(
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
                taskService.listProjectAttachments(StpUtil.getLoginIdAsLong(), id)
        );
    }
}
