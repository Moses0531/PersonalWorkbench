package com.moses.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.config.ResultConfig;
import com.moses.entity.WbMeeting;
import com.moses.utils.AliCloudUtil;
import com.moses.utils.UploadFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WbMeetingServiceImplTest {

    @Mock
    private UploadFileUtil uploadFileUtil;

    @Mock
    private AliCloudUtil aliCloudUtil;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    @InjectMocks
    private WbMeetingServiceImpl service;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void stubPersist() {
        lenient().doReturn(true).when(service).updateById(any(WbMeeting.class));
    }

    @Test
    void requireOwnedMeeting_nullId_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.requireOwnedMeeting(USER_ID, null));
        assertEquals("会议 ID 不能为空", ex.getMessage());
    }

    @Test
    void requireOwnedMeeting_notFound_throws() {
        doReturn(null).when(service).getOne(any());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.requireOwnedMeeting(USER_ID, 8L));
        assertEquals("会议不存在", ex.getMessage());
    }

    @Test
    void parseAttachments_blankOrInvalid_returnsEmpty() {
        assertTrue(service.parseAttachments(null).isEmpty());
        assertTrue(service.parseAttachments("").isEmpty());
        assertTrue(service.parseAttachments("{bad").isEmpty());
    }

    @Test
    void parseAttachments_validJson() {
        List<Map<String, Object>> list = service.parseAttachments("[{\"id\":\"m1\",\"name\":\"a.md\"}]");
        assertEquals(1, list.size());
        assertEquals("m1", list.get(0).get("id"));
    }

    @Test
    void uploadAttachment_empty_throws() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.md", "text/markdown", new byte[0]);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadAttachment(USER_ID, 1L, empty));
        assertEquals("会议材料不能为空", ex.getMessage());
    }

    @Test
    void uploadAttachment_success() throws Exception {
        WbMeeting meeting = ownedMeeting(5L, "[]");
        doReturn(meeting).when(service).getOne(any());
        doReturn(ResultConfig.success("https://oss/a.md")).when(uploadFileUtil).uploadToAliYun(any());

        MockMultipartFile file = new MockMultipartFile("file", "a.md", "text/markdown", "x".getBytes());
        Map<String, Object> item = service.uploadAttachment(USER_ID, 5L, file);
        assertEquals("a.md", item.get("name"));
        verify(service).updateById(any(WbMeeting.class));
    }

    @Test
    void removeAttachment_notFound_throws() {
        WbMeeting meeting = ownedMeeting(5L, "[{\"id\":\"m1\"}]");
        doReturn(meeting).when(service).getOne(any());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.removeAttachment(USER_ID, 5L, "missing"));
        assertEquals("材料不存在", ex.getMessage());
    }

    @Test
    void removeAttachment_success() {
        WbMeeting meeting = ownedMeeting(5L, "[{\"id\":\"m1\",\"name\":\"a.md\"}]");
        doReturn(meeting).when(service).getOne(any());
        service.removeAttachment(USER_ID, 5L, "m1");
        verify(service).updateById(any(WbMeeting.class));
    }

    @Test
    void applyAiSummary_blank_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.applyAiSummary(USER_ID, 5L, "  "));
        assertEquals("会议概要不能为空", ex.getMessage());
    }

    @Test
    void applyAiSummary_successWithFile() throws Exception {
        WbMeeting meeting = ownedMeeting(5L, "[]");
        meeting.setTitle("周会");
        doReturn(meeting).when(service).getOne(any());
        when(aliCloudUtil.upload(any(byte[].class), anyString())).thenReturn("https://oss/summary.md");

        Map<String, Object> file = service.applyAiSummary(USER_ID, 5L, "## 纪要");
        assertNotNull(file);
        assertEquals("ai-summary", file.get("kind"));
        assertEquals("1", meeting.getStatus());
        assertEquals("## 纪要", meeting.getAiSummary());
        verify(service).updateById(meeting);
    }

    @Test
    void applyAiSummary_uploadFailStillSavesText() throws Exception {
        WbMeeting meeting = ownedMeeting(5L, "[]");
        doReturn(meeting).when(service).getOne(any());
        when(aliCloudUtil.upload(any(byte[].class), anyString())).thenThrow(new RuntimeException("oss down"));

        Map<String, Object> file = service.applyAiSummary(USER_ID, 5L, "概要内容");
        assertNull(file);
        assertEquals("1", meeting.getStatus());
        assertEquals("概要内容", meeting.getAiSummary());
        verify(service).updateById(meeting);
    }

    private static WbMeeting ownedMeeting(Long id, String attachments) {
        WbMeeting m = new WbMeeting();
        m.setMeetingId(id);
        m.setUserId(USER_ID);
        m.setTitle("会议");
        m.setAttachments(attachments);
        m.setStatus("0");
        return m;
    }
}
