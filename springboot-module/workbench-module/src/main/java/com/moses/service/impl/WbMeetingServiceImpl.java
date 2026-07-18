package com.moses.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.config.ResultConfig;
import com.moses.entity.WbMeeting;
import com.moses.mapper.WbMeetingMapper;
import com.moses.service.WbMeetingService;
import com.moses.utils.AliCloudUtil;
import com.moses.utils.UploadFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 会议记录 Service 实现
 */
@Slf4j
@Service
public class WbMeetingServiceImpl extends ServiceImpl<WbMeetingMapper, WbMeeting>
        implements WbMeetingService {

    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;
    private static final int MAX_ATTACHMENT_COUNT = 20;
    private static final String KIND_AI_SUMMARY = "ai-summary";

    @Autowired
    private UploadFileUtil uploadFileUtil;

    @Autowired
    private AliCloudUtil aliCloudUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadAttachment(Long userId, Long meetingId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("会议材料不能为空");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new RuntimeException("单个文件不能超过 20MB");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            throw new RuntimeException("文件名须包含扩展名");
        }

        WbMeeting meeting = requireOwnedMeeting(userId, meetingId);
        List<Map<String, Object>> list = parseAttachments(meeting.getAttachments());
        if (list.size() >= MAX_ATTACHMENT_COUNT) {
            throw new RuntimeException("单场会议最多 " + MAX_ATTACHMENT_COUNT + " 个材料");
        }

        try {
            ResultConfig uploadResult = uploadFileUtil.uploadToAliYun(file);
            if (uploadResult.getCode() == null || uploadResult.getCode() != 1) {
                throw new RuntimeException(uploadResult.getMsg() != null ? uploadResult.getMsg() : "材料上传失败");
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", UUID.randomUUID().toString().replace("-", ""));
            item.put("name", originalName);
            item.put("url", String.valueOf(uploadResult.getData()));
            item.put("size", file.getSize());
            item.put("mime", file.getContentType() != null ? file.getContentType() : "");
            item.put("createTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            list.add(item);
            meeting.setAttachments(writeAttachments(list));
            meeting.setUpdateTime(new Date());
            updateById(meeting);
            return item;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("材料上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAttachment(Long userId, Long meetingId, String attachmentId) {
        if (!StringUtils.hasText(attachmentId)) {
            throw new RuntimeException("材料 ID 不能为空");
        }
        WbMeeting meeting = requireOwnedMeeting(userId, meetingId);
        List<Map<String, Object>> list = parseAttachments(meeting.getAttachments());
        boolean removed = list.removeIf(a -> attachmentId.equals(String.valueOf(a.get("id"))));
        if (!removed) {
            throw new RuntimeException("材料不存在");
        }
        meeting.setAttachments(writeAttachments(list));
        meeting.setUpdateTime(new Date());
        updateById(meeting);
    }

    @Override
    public WbMeeting requireOwnedMeeting(Long userId, Long meetingId) {
        if (meetingId == null) {
            throw new RuntimeException("会议 ID 不能为空");
        }
        WbMeeting meeting = getOne(
                new LambdaQueryWrapper<WbMeeting>()
                        .eq(WbMeeting::getMeetingId, meetingId)
                        .eq(WbMeeting::getUserId, userId)
        );
        if (meeting == null) {
            throw new RuntimeException("会议不存在");
        }
        return meeting;
    }

    @Override
    public List<Map<String, Object>> parseAttachments(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyAiSummary(Long userId, Long meetingId, String summary) {
        if (!StringUtils.hasText(summary)) {
            throw new RuntimeException("会议概要不能为空");
        }
        WbMeeting meeting = requireOwnedMeeting(userId, meetingId);
        String text = summary.trim();
        meeting.setAiSummary(text);
        meeting.setStatus("1");
        meeting.setUpdateTime(new Date());

        Map<String, Object> summaryFile = null;
        try {
            summaryFile = uploadAiSummaryFile(meeting, text);
            if (summaryFile != null) {
                List<Map<String, Object>> list = parseAttachments(meeting.getAttachments());
                list.removeIf(a -> KIND_AI_SUMMARY.equals(String.valueOf(a.get("kind"))));
                if (list.size() >= MAX_ATTACHMENT_COUNT) {
                    // 腾出一个名额给纪要文件
                    for (Iterator<Map<String, Object>> it = list.iterator(); it.hasNext(); ) {
                        Map<String, Object> item = it.next();
                        if (!KIND_AI_SUMMARY.equals(String.valueOf(item.get("kind")))) {
                            it.remove();
                            break;
                        }
                    }
                }
                list.add(summaryFile);
                meeting.setAttachments(writeAttachments(list));
            }
        } catch (Exception e) {
            // 概要文本已保存，文件生成失败不阻断整理结果
            log.warn("AI 纪要文件上传失败, meetingId={}: {}", meetingId, e.getMessage());
            summaryFile = null;
        }

        updateById(meeting);
        return summaryFile;
    }

    private Map<String, Object> uploadAiSummaryFile(WbMeeting meeting, String summary) throws Exception {
        String baseName = sanitizeFileName(meeting.getTitle());
        if (!StringUtils.hasText(baseName)) {
            baseName = "会议纪要";
        }
        String fileName = baseName + "-会议纪要.md";
        byte[] bytes = summary.getBytes(StandardCharsets.UTF_8);
        String url = aliCloudUtil.upload(bytes, fileName);

        Map<String, Object> item = new HashMap<>();
        item.put("id", UUID.randomUUID().toString().replace("-", ""));
        item.put("name", fileName);
        item.put("url", url);
        item.put("size", (long) bytes.length);
        item.put("mime", "text/markdown");
        item.put("kind", KIND_AI_SUMMARY);
        item.put("createTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return item;
    }

    private static String sanitizeFileName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String name = raw.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]+", "_");
        name = name.replaceAll("\\s+", " ").trim();
        if (name.length() > 40) {
            name = name.substring(0, 40).trim();
        }
        return name;
    }

    private String writeAttachments(List<Map<String, Object>> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            throw new RuntimeException("材料数据序列化失败");
        }
    }
}
