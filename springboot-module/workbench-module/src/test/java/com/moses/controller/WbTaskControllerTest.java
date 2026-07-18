package com.moses.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moses.config.ResultConfig;
import com.moses.entity.WbTask;
import com.moses.service.WbTaskService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WbTaskControllerTest {

    @Mock
    private WbTaskService wbTaskService;

    @InjectMocks
    private WbTaskController controller;

    @Test
    void addTask_bindsUserAndClearsAttachments() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(wbTaskService.save(any(WbTask.class))).thenReturn(true);

            WbTask body = new WbTask();
            body.setTaskId(9L);
            body.setAttachments("[{\"id\":1}]");
            ResultConfig result = controller.addTask(body);

            assertEquals(1, result.getCode());
            assertNull(body.getTaskId());
            assertEquals(3L, body.getUserId());
            assertNull(body.getAttachments());
        }
    }

    @Test
    void updateTask_clearsAttachmentsField() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(wbTaskService.update(any(WbTask.class), any())).thenReturn(true);

            WbTask body = new WbTask();
            body.setTaskId(9L);
            body.setAttachments("hack");
            controller.updateTask(body);
            assertEquals(3L, body.getUserId());
            assertNull(body.getAttachments());
        }
    }

    @Test
    void attachmentApis_passLoginUserId() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
            Map<String, Object> uploaded = Map.of("id", "a1");
            when(wbTaskService.uploadAttachment(3L, 9L, file)).thenReturn(uploaded);
            when(wbTaskService.updateAttachmentStatus(3L, 9L, "a1", "1")).thenReturn(uploaded);
            when(wbTaskService.listProjectAttachments(3L, 20L)).thenReturn(List.of(uploaded));

            assertEquals(uploaded, controller.uploadAttachment(9L, file).getData());

            Map<String, Object> removeBody = new HashMap<>();
            removeBody.put("taskId", 9L);
            removeBody.put("attachmentId", "a1");
            controller.removeAttachment(removeBody);
            verify(wbTaskService).removeAttachment(3L, 9L, "a1");

            Map<String, Object> statusBody = new HashMap<>();
            statusBody.put("taskId", 9L);
            statusBody.put("attachmentId", "a1");
            statusBody.put("status", "1");
            assertEquals(1, controller.updateAttachmentStatus(statusBody).getCode());

            assertEquals(1, controller.listProjectAttachments(20L).getCode());
        }
    }

    @Test
    void pageAndDelete_useLoginUser() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(wbTaskService.page(any(Page.class), any())).thenReturn(new Page<>());
            when(wbTaskService.remove(any())).thenReturn(true);
            assertEquals(1, controller.getTaskPage(1, 20).getCode());
            assertEquals(1, controller.deleteBatchTask(List.of(1L)).getCode());
        }
    }
}
