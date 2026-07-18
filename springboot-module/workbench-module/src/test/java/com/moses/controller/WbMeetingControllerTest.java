package com.moses.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbMeeting;
import com.moses.service.WbMeetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WbMeetingControllerTest {

    @Mock
    private WbMeetingService wbMeetingService;

    @InjectMocks
    private WbMeetingController controller;

    @Test
    void addMeeting_requiresTitle() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            WbMeeting body = new WbMeeting();
            body.setTitle("  ");
            ResultConfig result = controller.addMeeting(body);
            assertEquals(0, result.getCode());
            assertEquals("请填写会议标题", result.getMsg());
            verify(wbMeetingService, never()).save(any());
        }
    }

    @Test
    void addMeeting_bindsUserAndDefaults() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(5L);
            when(wbMeetingService.save(any(WbMeeting.class))).thenReturn(true);

            WbMeeting body = new WbMeeting();
            body.setTitle("周会");
            body.setMeetingId(1L);
            body.setAttachments("x");
            body.setAiSummary("y");
            ResultConfig result = controller.addMeeting(body);

            assertEquals(1, result.getCode());
            assertNull(body.getMeetingId());
            assertEquals(5L, body.getUserId());
            assertNull(body.getAttachments());
            assertNull(body.getAiSummary());
            assertEquals("0", body.getStatus());
            assertNotNull(body.getMeetingTime());
        }
    }

    @Test
    void updateMeeting_clearsSensitiveFields() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(5L);
            when(wbMeetingService.update(any(WbMeeting.class), any())).thenReturn(true);

            WbMeeting body = new WbMeeting();
            body.setMeetingId(9L);
            body.setAttachments("hack");
            body.setAiSummary("hack");
            controller.updateMeeting(body);
            assertEquals(5L, body.getUserId());
            assertNull(body.getAttachments());
            assertNull(body.getAiSummary());
        }
    }

    @Test
    void attachmentApis_passLoginUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(5L);
            MockMultipartFile file = new MockMultipartFile("file", "a.md", "text/markdown", "x".getBytes());
            Map<String, Object> uploaded = Map.of("id", "m1");
            when(wbMeetingService.uploadAttachment(5L, 9L, file)).thenReturn(uploaded);

            assertEquals(uploaded, controller.uploadAttachment(9L, file).getData());

            Map<String, Object> body = new HashMap<>();
            body.put("meetingId", 9L);
            body.put("attachmentId", "m1");
            controller.removeAttachment(body);
            verify(wbMeetingService).removeAttachment(5L, 9L, "m1");
        }
    }

    @Test
    void pageAndDelete_useLoginUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(5L);
            when(wbMeetingService.page(any(Page.class), any())).thenReturn(new Page<>());
            when(wbMeetingService.remove(any())).thenReturn(true);
            assertEquals(1, controller.getMeetingPage(1, 10).getCode());
            assertEquals(1, controller.deleteBatchMeeting(List.of(1L)).getCode());
        }
    }
}
