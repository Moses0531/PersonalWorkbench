package com.moses.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moses.entity.WbNote;
import com.moses.mapper.WbNoteMapper;
import com.moses.service.WbNoteService;
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
public class WbNoteServiceImpl extends ServiceImpl<WbNoteMapper, WbNote> implements WbNoteService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int AUTO_TITLE_LENGTH = 40;

    @Override
    public WbNote requireOwnedNote(Long userId, Long noteId) {
        if (noteId == null) {
            throw new RuntimeException("笔记 ID 不能为空");
        }
        WbNote note = getOne(
                new LambdaQueryWrapper<WbNote>()
                        .eq(WbNote::getNoteId, noteId)
                        .eq(WbNote::getUserId, userId)
        );
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        return note;
    }

    @Override
    public IPage<WbNote> pageOwnedNotes(Long userId, int pageNum, int pageRows,
                                        String keyword, String noteType, String status) {
        LambdaQueryWrapper<WbNote> qw = new LambdaQueryWrapper<WbNote>()
                .eq(WbNote::getUserId, userId)
                .orderByDesc(WbNote::getUpdateTime);

        if (StringUtils.hasText(noteType)) {
            qw.eq(WbNote::getNoteType, noteType.trim());
        }
        if (StringUtils.hasText(status)) {
            qw.eq(WbNote::getStatus, status.trim());
        } else {
            // 默认不展示已归档
            qw.eq(WbNote::getStatus, WbNote.STATUS_ACTIVE);
        }

        applyKeywordFilter(qw, keyword);
        return page(new Page<>(pageNum, pageRows), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WbNote createOwnedNote(Long userId, WbNote note) {
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
            note.setStatus(WbNote.STATUS_ACTIVE);
        }
        if (WbNote.TYPE_SCRAP.equals(noteType)) {
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
    public void updateOwnedNote(Long userId, WbNote note) {
        if (note == null || note.getNoteId() == null) {
            throw new RuntimeException("笔记 ID 不能为空");
        }
        WbNote existing = requireOwnedNote(userId, note.getNoteId());
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
                new LambdaQueryWrapper<WbNote>()
                        .eq(WbNote::getNoteId, note.getNoteId())
                        .eq(WbNote::getUserId, userId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedNotes(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        remove(
                new LambdaQueryWrapper<WbNote>()
                        .eq(WbNote::getUserId, userId)
                        .in(WbNote::getNoteId, ids)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveOwnedNote(Long userId, Long noteId, String status) {
        WbNote note = requireOwnedNote(userId, noteId);
        String flag = WbNote.STATUS_ARCHIVED.equals(String.valueOf(status).trim())
                ? WbNote.STATUS_ARCHIVED
                : WbNote.STATUS_ACTIVE;
        note.setStatus(flag);
        note.setUpdateTime(new Date());
        updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkTask(Long userId, Long noteId, Long taskId) {
        WbNote note = requireOwnedNote(userId, noteId);
        note.setLinkedTaskId(taskId);
        note.setUpdateTime(new Date());
        updateById(note);
    }

    @Override
    public List<WbNote> listOwnedByIds(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return list(
                new LambdaQueryWrapper<WbNote>()
                        .eq(WbNote::getUserId, userId)
                        .in(WbNote::getNoteId, ids)
        );
    }

    private void applyKeywordFilter(LambdaQueryWrapper<WbNote> qw, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        List<String> tokens = Arrays.stream(keyword.trim().split("\\s+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        for (String token : tokens) {
            qw.and(w -> w.like(WbNote::getTitle, token).or().like(WbNote::getContent, token));
        }
    }

    private static String normalizeNoteType(String raw) {
        if (WbNote.TYPE_DIGEST.equals(raw)) {
            return WbNote.TYPE_DIGEST;
        }
        return WbNote.TYPE_SCRAP;
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
