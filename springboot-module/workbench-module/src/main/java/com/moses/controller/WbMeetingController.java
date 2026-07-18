package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbMeeting;
import com.moses.service.WbMeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wbMeeting")
@Tag(name = "工作台-会议", description = "会议记录与材料相关接口")
public class WbMeetingController {

    @Autowired
    private WbMeetingService wbMeetingService;

    @GetMapping("/getMeetingPage")
    @Operation(summary = "获取会议分页列表", description = "仅返回当前登录用户的会议")
    @SaCheckPermission("meeting:query")
    public ResultConfig getMeetingPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<WbMeeting> page = wbMeetingService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<WbMeeting>()
                        .eq(WbMeeting::getUserId, userId)
                        .orderByDesc(WbMeeting::getMeetingTime)
        );
        return ResultConfig.success(page);
    }

    @PostMapping("/addMeeting")
    @Operation(summary = "新增会议")
    @SaCheckPermission("meeting:add")
    public ResultConfig addMeeting(@RequestBody WbMeeting wbMeeting) {
        if (!StringUtils.hasText(wbMeeting.getTitle())) {
            return ResultConfig.error("请填写会议标题");
        }
        if (wbMeeting.getMeetingTime() == null) {
            wbMeeting.setMeetingTime(new Date());
        }
        wbMeeting.setMeetingId(null);
        wbMeeting.setUserId(StpUtil.getLoginIdAsLong());
        wbMeeting.setAttachments(null);
        wbMeeting.setAiSummary(null);
        if (!StringUtils.hasText(wbMeeting.getStatus())) {
            wbMeeting.setStatus("0");
        }
        wbMeetingService.save(wbMeeting);
        return ResultConfig.success(wbMeeting);
    }

    @PostMapping("/updateMeeting")
    @Operation(summary = "修改会议")
    @SaCheckPermission("meeting:modify")
    public ResultConfig updateMeeting(@RequestBody WbMeeting wbMeeting) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbMeeting.setUserId(userId);
        // 材料与 AI 概要仅走专用接口
        wbMeeting.setAttachments(null);
        wbMeeting.setAiSummary(null);
        wbMeetingService.update(
                wbMeeting,
                new LambdaQueryWrapper<WbMeeting>()
                        .eq(WbMeeting::getMeetingId, wbMeeting.getMeetingId())
                        .eq(WbMeeting::getUserId, userId)
        );
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchMeeting")
    @Operation(summary = "批量删除会议")
    @SaCheckPermission("meeting:remove")
    public ResultConfig deleteBatchMeeting(@RequestBody List<Long> ids) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbMeetingService.remove(
                new LambdaQueryWrapper<WbMeeting>()
                        .in(WbMeeting::getMeetingId, ids)
                        .eq(WbMeeting::getUserId, userId)
        );
        return ResultConfig.success();
    }

    @PostMapping("/uploadAttachment")
    @Operation(summary = "上传会议材料（阿里云 OSS）")
    @SaCheckPermission("meeting:modify")
    public ResultConfig uploadAttachment(@RequestParam("meetingId") Long meetingId,
                                         @RequestParam("file") MultipartFile file) {
        return ResultConfig.success(
                wbMeetingService.uploadAttachment(StpUtil.getLoginIdAsLong(), meetingId, file)
        );
    }

    @DeleteMapping("/removeAttachment")
    @Operation(summary = "删除会议材料")
    @SaCheckPermission("meeting:modify")
    public ResultConfig removeAttachment(@RequestBody Map<String, Object> body) {
        Long meetingId = body.get("meetingId") != null ? Long.valueOf(String.valueOf(body.get("meetingId"))) : null;
        String attachmentId = body.get("attachmentId") != null ? String.valueOf(body.get("attachmentId")) : null;
        wbMeetingService.removeAttachment(StpUtil.getLoginIdAsLong(), meetingId, attachmentId);
        return ResultConfig.success();
    }
}
