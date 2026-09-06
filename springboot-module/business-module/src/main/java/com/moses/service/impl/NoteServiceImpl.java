package com.moses.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.entity.Note;
import com.moses.mapper.NoteMapper;
import com.moses.service.NoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记 Service 实现
 */
@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int AUTO_TITLE_LENGTH = 40;

    @Override
    public Note requireOwnedNote(Long userId, Long noteId) {
        if (noteId == null) {
            throw new RuntimeException("笔记 ID 不能为空");
        }
        Note note = getOne(
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getNoteId, noteId)
                        .eq(Note::getUserId, userId)
        );
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        return note;
    }

    @Override
    public IPage<Note> pageOwnedNotes(Long userId, int pageNum, int pageRows,
                                        String keyword, String noteType, String status) {
        LambdaQueryWrapper<Note> qw = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByDesc(Note::getUpdateTime);

        if (StringUtils.hasText(noteType)) {
            qw.eq(Note::getNoteType, noteType.trim());
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Note::getStatus, status.trim());
        } else {
            // 默认不展示已归档
            qw.eq(Note::getStatus, Note.STATUS_ACTIVE);
        }

        applyKeywordFilter(qw, keyword);
        return page(new Page<>(pageNum, pageRows), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Note createOwnedNote(Long userId, Note note) {
        if (note == null) {
            throw new RuntimeException("笔记不能为空");
        }
        String content = note.getContent() != null ? note.getContent().trim() : "";
        if (!StringUtils.hasText(content) && !StringUtils.hasText(note.getTitle())) {
            throw new RuntimeException("请填写标题或正文");
        }

        String noteType = normalizeNoteType(note.getNoteType());
        note.setNoteId(null);
        note.setUserId(userId);
        note.setNoteType(noteType);
        note.setContent(content);
        note.setTitle(resolveTitle(note.getTitle(), content));
        if (!StringUtils.hasText(note.getStatus())) {
            note.setStatus(Note.STATUS_ACTIVE);
        }
        if (Note.TYPE_SCRAP.equals(noteType)) {
            note.setSourceNoteIds(null);
            note.setLinkedTaskId(null);
        }
        Date now = new Date();
        note.setCreateTime(now);
        note.setUpdateTime(now);
        save(note);
        return note;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOwnedNote(Long userId, Note note) {
        if (note == null || note.getNoteId() == null) {
            throw new RuntimeException("笔记 ID 不能为空");
        }
        Note existing = requireOwnedNote(userId, note.getNoteId());
        note.setUserId(userId);
        note.setNoteType(existing.getNoteType());
        note.setSourceNoteIds(existing.getSourceNoteIds());
        note.setLinkedTaskId(existing.getLinkedTaskId());
        if (!StringUtils.hasText(note.getStatus())) {
            note.setStatus(existing.getStatus());
        }
        String content = note.getContent() != null ? note.getContent().trim() : "";
        note.setContent(content);
        note.setTitle(resolveTitle(note.getTitle(), content));
        note.setUpdateTime(new Date());
        update(
                note,
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getNoteId, note.getNoteId())
                        .eq(Note::getUserId, userId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedNotes(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        remove(
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .in(Note::getNoteId, ids)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveOwnedNote(Long userId, Long noteId, String status) {
        Note note = requireOwnedNote(userId, noteId);
        String flag = Note.STATUS_ARCHIVED.equals(String.valueOf(status).trim())
                ? Note.STATUS_ARCHIVED
                : Note.STATUS_ACTIVE;
        note.setStatus(flag);
        note.setUpdateTime(new Date());
        updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTask(Long userId, Long noteId, Long taskId) {
        Note note = requireOwnedNote(userId, noteId);
        note.setLinkedTaskId(taskId);
        note.setUpdateTime(new Date());
        updateById(note);
    }

    @Override
    public List<Note> listOwnedByIds(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list(
                new LambdaQueryWrapper<Note>()
                        .eq(Note::getUserId, userId)
                        .in(Note::getNoteId, ids)
        );
    }

    private void applyKeywordFilter(LambdaQueryWrapper<Note> qw, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        List<String> tokens = Arrays.stream(keyword.trim().split("\\s+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        for (String token : tokens) {
            qw.and(w -> w.like(Note::getTitle, token).or().like(Note::getContent, token));
        }
    }

    private static String normalizeNoteType(String raw) {
        if (Note.TYPE_DIGEST.equals(raw)) {
            return Note.TYPE_DIGEST;
        }
        return Note.TYPE_SCRAP;
    }

    private static String resolveTitle(String title, String content) {
        if (StringUtils.hasText(title)) {
            String t = title.trim();
            return t.length() > MAX_TITLE_LENGTH ? t.substring(0, MAX_TITLE_LENGTH) : t;
        }
        if (!StringUtils.hasText(content)) {
            return "未命名笔记";
        }
        String plain = content.replaceAll("\\s+", " ").trim();
        if (plain.length() > AUTO_TITLE_LENGTH) {
            return plain.substring(0, AUTO_TITLE_LENGTH) + "…";
        }
        return plain.isEmpty() ? "未命名笔记" : plain;
    }
}
