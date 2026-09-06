package com.moses.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.entity.Meeting;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 针对表【meeting(工作台-会议记录表)】的数据库操作 Service
 */
public interface MeetingService extends IService<Meeting> {

    Map<String, Object> uploadAttachment(Long userId, Long meetingId, MultipartFile file);

    void removeAttachment(Long userId, Long meetingId, String attachmentId);

    Meeting requireOwnedMeeting(Long userId, Long meetingId);

    List<Map<String, Object>> parseAttachments(String json);

    /**
     * 写入 AI 概要，并生成 Markdown 纪要文件加入材料列表。
     * @return 新生成的纪要文件元数据（上传失败时可能为 null）
     */
    Map<String, Object> applyAiSummary(Long userId, Long meetingId, String summary);
}
