package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.Event;
import com.moses.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event")
@Tag(name = "业务-日程", description = "个人日程相关接口")
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/getEventPage")
    @Operation(summary = "获取日程分页列表", description = "仅返回当前登录用户的日程")
    @SaCheckPermission("event:query")
    public ResultConfig getEventPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                     @RequestParam(value = "pageRows", defaultValue = "20") Integer pageRows) {
        Long userId = StpUtil.getLoginIdAsLong();
        IPage<Event> page = eventService.page(
                new Page<>(pageNum, pageRows),
                new LambdaQueryWrapper<Event>()
                        .eq(Event::getUserId, userId)
                        .orderByAsc(Event::getDisplayOrder)
                        .orderByAsc(Event::getStartTime)
        );
        return ResultConfig.success(page);
    }

    @PostMapping("/addEvent")
    @Operation(summary = "新增日程")
    @SaCheckPermission("event:add")
    public ResultConfig addEvent(@RequestBody Event event) {
        event.setEventId(null);
        event.setUserId(StpUtil.getLoginIdAsLong());
        eventService.save(event);
        return ResultConfig.success();
    }

    @PostMapping("/updateEvent")
    @Operation(summary = "修改日程")
    @SaCheckPermission("event:modify")
    public ResultConfig updateEvent(@RequestBody Event event) {
        Long userId = StpUtil.getLoginIdAsLong();
        event.setUserId(userId);
        eventService.update(
                event,
                new LambdaQueryWrapper<Event>()
                        .eq(Event::getEventId, event.getEventId())
                        .eq(Event::getUserId, userId)
        );
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchEvent")
    @Operation(summary = "批量删除日程")
    @SaCheckPermission("event:remove")
    public ResultConfig deleteBatchEvent(@RequestBody List<Long> ids) {
        Long userId = StpUtil.getLoginIdAsLong();
        eventService.remove(
                new LambdaQueryWrapper<Event>()
                        .in(Event::getEventId, ids)
                        .eq(Event::getUserId, userId)
        );
        return ResultConfig.success();
    }
}
