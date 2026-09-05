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
    void addTask_delegatesToCreateOwnedTask() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            doNothing().when(wbTaskService).createOwnedTask(eq(3L), any(WbTask.class));

            WbTask body = new WbTask();
            body.setTitle("demo");
            ResultConfig result = controller.addTask(body);

            assertEquals(1, result.getCode());
            verify(wbTaskService).createOwnedTask(eq(3L), same(body));
        }
    }

    @Test
    void updateTask_delegatesToUpdateOwnedTask() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            doNothing().when(wbTaskService).updateOwnedTask(eq(3L), any(WbTask.class));

            WbTask body = new WbTask();
            body.setTaskId(9L);
            body.setTitle("demo");
            controller.updateTask(body);
            verify(wbTaskService).updateOwnedTask(eq(3L), same(body));
        }
    }

    @Test
    void deleteBatch_cascades() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            doNothing().when(wbTaskService).deleteOwnedTasksCascade(eq(3L), anyList());
            controller.deleteBatchTask(List.of(1L, 2L));
            verify(wbTaskService).deleteOwnedTasksCascade(3L, List.of(1L, 2L));
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
            assertEquals(uploaded, controller.updateAttachmentStatus(statusBody).getData());

            assertEquals(List.of(uploaded), controller.listProjectAttachments(20L, null).getData());
        }
    }

    @Test
    void getTaskPage_ok() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(wbTaskService.page(any(), any())).thenReturn(new Page<>());
            assertEquals(1, controller.getTaskPage(1, 20, null, null, null).getCode());
        }
    }
}
