package com.moses.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.entity.WbNote;

import java.util.List;

/**
 * 针对表【wb_note(工作台-笔记)】的数据库操作 Service
 */
public interface WbNoteService extends IService<WbNote> {

    WbNote requireOwnedNote(Long userId, Long noteId);

    IPage<WbNote> pageOwnedNotes(Long userId, int pageNum, int pageRows,
                                 String keyword, String noteType, String status);

    WbNote createOwnedNote(Long userId, WbNote note);

    void updateOwnedNote(Long userId, WbNote note);

    void deleteOwnedNotes(Long userId, List<Long> ids);

    void archiveOwnedNote(Long userId, Long noteId, String status);

    void linkTask(Long userId, Long noteId, Long taskId);

    List<WbNote> listOwnedByIds(Long userId, List<Long> ids);
}
