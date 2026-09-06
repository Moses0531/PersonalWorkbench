package com.moses.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.moses.config.ResultConfig;
import com.moses.entity.Note;
import com.moses.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/note")
@Tag(name = "业务-笔记", description = "零散记录与整理稿相关接口")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping("/getNotePage")
    @Operation(summary = "获取笔记分页列表", description = "支持 keyword / noteType / status 筛选")
    @SaCheckPermission("note:query")
    public ResultConfig getNotePage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageRows", defaultValue = "50") Integer pageRows,
                                    @RequestParam(value = "keyword", required = false) String keyword,
                                    @RequestParam(value = "noteType", required = false) String noteType,
                                    @RequestParam(value = "status", required = false) String status) {
        return ResultConfig.success(
                noteService.pageOwnedNotes(
                        StpUtil.getLoginIdAsLong(), pageNum, pageRows, keyword, noteType, status)
        );
    }

    @GetMapping("/getNote")
    @Operation(summary = "获取单条笔记")
    @SaCheckPermission("note:query")
    public ResultConfig getNote(@RequestParam("noteId") Long noteId) {
        return ResultConfig.success(
                noteService.requireOwnedNote(StpUtil.getLoginIdAsLong(), noteId)
        );
    }

    @PostMapping("/addNote")
    @Operation(summary = "新增笔记")
    @SaCheckPermission("note:add")
    public ResultConfig addNote(@RequestBody Note note) {
        return ResultConfig.success(
                noteService.createOwnedNote(StpUtil.getLoginIdAsLong(), note)
        );
    }

    @PostMapping("/updateNote")
    @Operation(summary = "修改笔记")
    @SaCheckPermission("note:modify")
    public ResultConfig updateNote(@RequestBody Note note) {
        noteService.updateOwnedNote(StpUtil.getLoginIdAsLong(), note);
        return ResultConfig.success();
    }

    @DeleteMapping("/deleteBatchNote")
    @Operation(summary = "批量删除笔记")
    @SaCheckPermission("note:remove")
    public ResultConfig deleteBatchNote(@RequestBody List<Long> ids) {
        noteService.deleteOwnedNotes(StpUtil.getLoginIdAsLong(), ids);
        return ResultConfig.success();
    }

    @PostMapping("/archiveNote")
    @Operation(summary = "归档/取消归档笔记")
    @SaCheckPermission("note:modify")
    public ResultConfig archiveNote(@RequestBody Map<String, Object> body) {
        Long noteId = body != null && body.get("noteId") != null
                ? Long.valueOf(String.valueOf(body.get("noteId")))
                : null;
        String status = body != null && body.get("status") != null
                ? String.valueOf(body.get("status"))
                : Note.STATUS_ARCHIVED;
        noteService.archiveOwnedNote(StpUtil.getLoginIdAsLong(), noteId, status);
        return ResultConfig.success();
    }
}
