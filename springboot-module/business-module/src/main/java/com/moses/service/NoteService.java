package com.moses.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.entity.Note;

import java.util.List;

/**
 * 针对表【note(工作台-笔记)】的数据库操作 Service
 */
public interface NoteService extends IService<Note> {

    Note requireOwnedNote(Long userId, Long noteId);

    IPage<Note> pageOwnedNotes(Long userId, int pageNum, int pageRows,
                                 String keyword, String noteType, String status);

    Note createOwnedNote(Long userId, Note note);

    void updateOwnedNote(Long userId, Note note);

    void deleteOwnedNotes(Long userId, List<Long> ids);

    void archiveOwnedNote(Long userId, Long noteId, String status);

    void linkTask(Long userId, Long noteId, Long taskId);

    List<Note> listOwnedByIds(Long userId, List<Long> ids);
}
