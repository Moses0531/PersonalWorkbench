package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbEvent;
import com.moses.service.WbEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wbEvent")
@Tag(name = "工作台-日程", description = "个人日程相关接口")
public class WbEventController {

    @Autowired
    private WbEventService wbEventService;

    @GetMapping("/getEventPage")
    @Operation(summary = "获取日程分页列表", description = "仅返回当前登录用户的日程")
    @SaCheckPermission("event:query")
    public ResultConfig getEventPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                     @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<WbEvent> page = wbEventService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<WbEvent>()
                        .eq(WbEvent::getUserId, userId)
                        .orderByAsc(WbEvent::getDisplayOrder)
                        .orderByAsc(WbEvent::getStartTime)
        );
        return ResultConfig.success(page);
    }

    @PostMapping("/addEvent")
    @Operation(summary = "新增日程")
    @SaCheckPermission("event:add")
    public ResultConfig addEvent(@RequestBody WbEvent wbEvent) {
        wbEvent.setEventId(null);
        wbEvent.setUserId(StpUtil.getLoginIdAsLong());
        wbEventService.save(wbEvent);
        return ResultConfig.success();
    }

    @PostMapping("/updateEvent")
    @Operation(summary = "修改日程")
    @SaCheckPermission("event:modify")
    public ResultConfig updateEvent(@RequestBody WbEvent wbEvent) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbEvent.setUserId(userId);
        wbEventService.update(
                wbEvent,
                new LambdaQueryWrapper<WbEvent>()
                        .eq(WbEvent::getEventId, wbEvent.getEventId())
                        .eq(WbEvent::getUserId, userId)
        );
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchEvent")
    @Operation(summary = "批量删除日程")
    @SaCheckPermission("event:remove")
    public ResultConfig deleteBatchEvent(@RequestBody List<Long> ids) {
        Long userId = StpUtil.getLoginIdAsLong();
        wbEventService.remove(
                new LambdaQueryWrapper<WbEvent>()
                        .in(WbEvent::getEventId, ids)
                        .eq(WbEvent::getUserId, userId)
        );
        return ResultConfig.success();
    }
}
