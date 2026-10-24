package com.moses.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.config.ResultConfig;
import com.moses.task.entity.Task;
import com.moses.utils.UploadFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private UploadFileUtil uploadFileUtil;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    @InjectMocks
    private TaskServiceImpl service;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER = 99L;

    @BeforeEach
    void stubPersist() {
        lenient().doAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getTaskId() == null) {
                t.setTaskId(100L + (long) (Math.random() * 1000));
            }
            return true;
        }).when(service).save(any(Task.class));
        lenient().doReturn(true).when(service).updateById(any(Task.class));
    }

    @Test
    void uploadAttachment_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadAttachment(USER_ID, 1L, empty));
        assertEquals("附件不能为空", ex.getMessage());
    }

    @Test
    void uploadAttachment_noExtension_throws() {
        MockMultipartFile file = new MockMultipartFile("file", "readme", "text/plain", "hi".getBytes());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadAttachment(USER_ID, 1L, file));
        assertEquals("文件名须包含扩展名", ex.getMessage());
    }

    @Test
    void uploadAttachment_taskNotOwned_throws() {
        doReturn(null).when(service).getOne(any());
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadAttachment(USER_ID, 1L, file));
        assertEquals("任务不存在", ex.getMessage());
    }

    @Test
    void uploadAttachment_archivedProject_throws() {
        Task task = ownedTask(10L, 20L, Task.NODE_TASK, "[]");
        Task project = projectRoot(20L, "1");
        doReturn(task, project).when(service).getOne(any());

        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadAttachment(USER_ID, 10L, file));
        assertEquals("已归档，附件只读", ex.getMessage());
    }

    @Test
    void uploadAttachment_success() throws Exception {
        Task task = ownedTask(10L, null, Task.NODE_TASK, "[]");
        doReturn(task).when(service).getOne(any());
        doReturn(ResultConfig.success("https://oss/a.txt")).when(uploadFileUtil).uploadToAliYun(any());

        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());
        Map<String, Object> item = service.uploadAttachment(USER_ID, 10L, file);

        assertNotNull(item.get("id"));
        assertEquals("a.txt", item.get("name"));
        assertEquals("0", item.get("status"));
        verify(service).updateById(any(Task.class));
    }

    @Test
    void removeAttachment_missingId_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.removeAttachment(USER_ID, 1L, " "));
        assertEquals("附件 ID 不能为空", ex.getMessage());
    }

    @Test
    void removeAttachment_notFound_throws() {
        Task task = ownedTask(10L, null, Task.NODE_TASK, "[{\"id\":\"att1\",\"name\":\"a.txt\"}]");
        doReturn(task).when(service).getOne(any());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.removeAttachment(USER_ID, 10L, "missing"));
        assertEquals("附件不存在", ex.getMessage());
    }

    @Test
    void removeAttachment_success() {
        Task task = ownedTask(10L, null, Task.NODE_TASK, "[{\"id\":\"att1\",\"name\":\"a.txt\"}]");
        doReturn(task).when(service).getOne(any());
        service.removeAttachment(USER_ID, 10L, "att1");
        verify(service).updateById(argThat(t -> "[]".equals(t.getAttachments()) || "[]".equals(t.getAttachments().replace(" ", ""))));
    }

    @Test
    void updateAttachmentStatus_invalidStatus_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateAttachmentStatus(USER_ID, 1L, "att1", "9"));
        assertTrue(ex.getMessage().contains("附件状态无效"));
    }

    @Test
    void updateAttachmentStatus_success() {
        Task task = ownedTask(10L, null, Task.NODE_TASK, "[{\"id\":\"att1\",\"name\":\"a.txt\",\"status\":\"0\"}]");
        doReturn(task).when(service).getOne(any());

        Map<String, Object> updated = service.updateAttachmentStatus(USER_ID, 10L, "att1", "2");
        assertEquals("2", updated.get("status"));
        verify(service).updateById(any(Task.class));
    }

    @Test
    void listProjectAttachments_projectMissing_throws() {
        doReturn(null).when(service).getOne(any());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.listProjectAttachments(USER_ID, 20L));
        assertEquals("任务不存在", ex.getMessage());
    }

    @Test
    void listProjectAttachments_aggregatesAndMapsHandled() {
        Task project = projectRoot(20L, "0");
        doReturn(project).when(service).getOne(any());

        Task task = ownedTask(10L, 20L, Task.NODE_TASK, "[{\"id\":\"a1\",\"name\":\"x.pdf\",\"handled\":true}]");
        doReturn(List.of(task)).when(service).list(any(Wrapper.class));

        List<Map<String, Object>> rows = service.listProjectAttachments(USER_ID, 20L);
        assertEquals(1, rows.size());
        assertEquals("1", rows.get(0).get("status"));
        assertEquals(10L, rows.get(0).get("taskId"));
        assertFalse(rows.get(0).containsKey("handled"));
    }

    @Test
    void getProjectBoardSnapshot_countsByStatus() {
        Task project = projectRoot(20L, "0");
        project.setTitle("Demo");
        doReturn(project).when(service).getOne(any());

        Task t0 = ownedTask(1L, 20L, Task.NODE_TASK, null);
        t0.setStatus("0");
        Task t1 = ownedTask(2L, 20L, Task.NODE_TASK, null);
        t1.setStatus("1");
        Task t2 = ownedTask(3L, 20L, Task.NODE_TASK, null);
        t2.setStatus("2");
        Task t3 = ownedTask(4L, 20L, Task.NODE_TASK, null);
        t3.setStatus("3");
        doReturn(List.of(t0, t1, t2, t3)).when(service).list(any(Wrapper.class));

        Map<String, Object> snapshot = service.getProjectBoardSnapshot(USER_ID, 20L);
        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) snapshot.get("counts");
        assertEquals(4, counts.get("total"));
        assertEquals(1, counts.get("todo"));
        assertEquals(1, counts.get("doing"));
        assertEquals(1, counts.get("done"));
        assertEquals(1, counts.get("cancelled"));
        assertEquals(20L, snapshot.get("projectTaskId"));
    }

    @Test
    void applyPlanBatch_emptyPhases_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.applyPlanBatch(USER_ID, 20L, "batch-1", List.of()));
        assertEquals("规划阶段不能为空", ex.getMessage());
    }

    @Test
    void applyPlanBatch_archived_throws() {
        Task archived = projectRoot(20L, "1");
        doReturn(archived).when(service).getOne(any());

        Map<String, Object> phase = new HashMap<>();
        phase.put("title", "阶段一");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.applyPlanBatch(USER_ID, 20L, "batch-1", List.of(phase)));
        assertEquals("已归档，无法写入规划任务", ex.getMessage());
    }

    @Test
    void applyPlanBatch_createsParentAndChild() {
        Task project = projectRoot(20L, "0");
        doReturn(project).when(service).getOne(any());

        Map<String, Object> step = new HashMap<>();
        step.put("title", "步骤A");
        step.put("status", "9");
        Map<String, Object> phase = new HashMap<>();
        phase.put("title", "阶段一");
        phase.put("steps", List.of(step));

        Map<String, Object> result = service.applyPlanBatch(USER_ID, 20L, " batch-1 ", List.of(phase));
        assertEquals(2, result.get("created"));
        assertEquals("batch-1", result.get("planBatchId"));
        assertEquals(20L, result.get("projectTaskId"));
        verify(service, times(2)).save(any(Task.class));
    }

    @Test
    void crossUser_taskLooksMissing() {
        doReturn(null).when(service).getOne(any());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.removeAttachment(OTHER_USER, 10L, "att1"));
        assertEquals("任务不存在", ex.getMessage());
    }

    private static Task projectRoot(Long taskId, String archived) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setUserId(USER_ID);
        task.setNodeType(Task.NODE_PROJECT);
        task.setArchived(archived);
        task.setParentTaskId(null);
        task.setTitle("项目" + taskId);
        task.setStatus("0");
        return task;
    }

    private static Task ownedTask(Long taskId, Long parentTaskId, String nodeType, String attachments) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setUserId(USER_ID);
        task.setNodeType(nodeType);
        task.setArchived("0");
        task.setParentTaskId(parentTaskId);
        task.setTitle("任务" + taskId);
        task.setAttachments(attachments);
        task.setStatus("0");
        return task;
    }
}
