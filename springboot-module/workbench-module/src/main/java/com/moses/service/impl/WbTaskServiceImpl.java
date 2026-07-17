package com.moses.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.config.ResultConfig;
import com.moses.entity.WbProject;
import com.moses.entity.WbTask;
import com.moses.mapper.WbTaskMapper;
import com.moses.service.WbProjectService;
import com.moses.service.WbTaskService;
import com.moses.utils.UploadFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Administrator
 * @description 针对表【wb_task(工作台-任务表（独立任务或项目子任务）)】的数据库操作Service实现
 * @createDate 2026-07-16 22:59:29
 */
@Service
public class WbTaskServiceImpl extends ServiceImpl<WbTaskMapper, WbTask>
        implements WbTaskService {

    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;
    private static final int MAX_ATTACHMENT_COUNT = 20;

    @Autowired
    private UploadFileUtil uploadFileUtil;

    @Autowired
    private WbProjectService wbProjectService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadAttachment(Long userId, Long taskId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("附件不能为空");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new RuntimeException("单个附件不能超过 20MB");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            throw new RuntimeException("文件名须包含扩展名");
        }

        WbTask task = requireOwnedTask(userId, taskId);
        assertAttachmentsWritable(userId, task);

        List<Map<String, Object>> list = parseAttachments(task.getAttachments());
        if (list.size() >= MAX_ATTACHMENT_COUNT) {
            throw new RuntimeException("单个任务最多 " + MAX_ATTACHMENT_COUNT + " 个附件");
        }

        try {
            ResultConfig uploadResult = uploadFileUtil.uploadToAliYun(file);
            if (uploadResult.getCode() == null || uploadResult.getCode() != 1) {
                throw new RuntimeException(uploadResult.getMsg() != null ? uploadResult.getMsg() : "附件上传失败");
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", UUID.randomUUID().toString().replace("-", ""));
            item.put("name", originalName);
            item.put("url", String.valueOf(uploadResult.getData()));
            item.put("size", file.getSize());
            item.put("mime", file.getContentType() != null ? file.getContentType() : "");
            item.put("createTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            list.add(item);
            task.setAttachments(writeAttachments(list));
            task.setUpdateTime(new Date());
            updateById(task);
            return item;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("附件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAttachment(Long userId, Long taskId, String attachmentId) {
        if (!StringUtils.hasText(attachmentId)) {
            throw new RuntimeException("附件 ID 不能为空");
        }
        WbTask task = requireOwnedTask(userId, taskId);
        assertAttachmentsWritable(userId, task);

        List<Map<String, Object>> list = parseAttachments(task.getAttachments());
        boolean removed = list.removeIf(a -> attachmentId.equals(String.valueOf(a.get("id"))));
        if (!removed) {
            throw new RuntimeException("附件不存在");
        }
        task.setAttachments(writeAttachments(list));
        task.setUpdateTime(new Date());
        updateById(task);
    }

    @Override
    public List<Map<String, Object>> listProjectAttachments(Long userId, Long projectId) {
        if (projectId == null) {
            throw new RuntimeException("项目 ID 不能为空");
        }
        WbProject project = wbProjectService.getOne(
                new LambdaQueryWrapper<WbProject>()
                        .eq(WbProject::getProjectId, projectId)
                        .eq(WbProject::getUserId, userId)
        );
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        List<WbTask> tasks = list(
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getUserId, userId)
                        .eq(WbTask::getProjectId, projectId)
                        .orderByAsc(WbTask::getDisplayOrder)
                        .orderByDesc(WbTask::getUpdateTime)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (WbTask task : tasks) {
            for (Map<String, Object> item : parseAttachments(task.getAttachments())) {
                Map<String, Object> row = new HashMap<>(item);
                row.put("taskId", task.getTaskId());
                row.put("taskTitle", task.getTitle());
                result.add(row);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getProjectBoardSnapshot(Long userId, Long projectId) {
        WbProject project = requireOwnedProject(userId, projectId);
        List<WbTask> tasks = list(
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getUserId, userId)
                        .eq(WbTask::getProjectId, projectId)
                        .orderByAsc(WbTask::getDisplayOrder)
                        .orderByDesc(WbTask::getUpdateTime)
        );

        List<Map<String, Object>> taskSummaries = new ArrayList<>();
        int todo = 0;
        int doing = 0;
        int done = 0;
        int cancelled = 0;
        for (WbTask task : tasks) {
            String status = task.getStatus() != null ? String.valueOf(task.getStatus()) : "0";
            switch (status) {
                case "1" -> doing++;
                case "2" -> done++;
                case "3" -> cancelled++;
                default -> todo++;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("taskId", task.getTaskId());
            row.put("parentTaskId", task.getParentTaskId());
            row.put("planBatchId", task.getPlanBatchId());
            row.put("title", task.getTitle());
            row.put("status", status);
            row.put("priority", task.getPriority());
            row.put("dueTime", task.getDueTime());
            taskSummaries.add(row);
        }

        Map<String, Object> counts = new HashMap<>();
        counts.put("total", tasks.size());
        counts.put("todo", todo);
        counts.put("doing", doing);
        counts.put("done", done);
        counts.put("cancelled", cancelled);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("projectId", project.getProjectId());
        snapshot.put("name", project.getName());
        snapshot.put("description", project.getDescription());
        snapshot.put("status", project.getStatus());
        snapshot.put("counts", counts);
        snapshot.put("tasks", taskSummaries);
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public Map<String, Object> applyPlanBatch(Long userId, Long projectId, String planBatchId,
                                              List<Map<String, Object>> phases) {
        if (!StringUtils.hasText(planBatchId)) {
            throw new RuntimeException("规划批次号不能为空");
        }
        if (phases == null || phases.isEmpty()) {
            throw new RuntimeException("规划阶段不能为空");
        }
        WbProject project = requireOwnedProject(userId, projectId);
        if ("1".equals(String.valueOf(project.getStatus()))) {
            throw new RuntimeException("项目已归档，无法写入规划任务");
        }

        int created = 0;
        int phaseOrder = 0;
        for (Map<String, Object> phase : phases) {
            if (phase == null) {
                continue;
            }
            String title = str(phase.get("title"));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            WbTask parent = new WbTask();
            parent.setUserId(userId);
            parent.setProjectId(projectId);
            parent.setPlanBatchId(planBatchId.trim());
            parent.setTitle(title);
            parent.setDescription(str(phase.get("description")));
            parent.setDueTime(toDate(phase.get("dueTime")));
            parent.setStatus("0");
            parent.setDisplayOrder(phaseOrder++);
            parent.setRemark("AI规划·阶段");
            save(parent);
            created++;

            Object stepsObj = phase.get("steps");
            if (!(stepsObj instanceof List<?> steps) || steps.isEmpty()) {
                continue;
            }
            int stepOrder = 0;
            for (Object stepObj : steps) {
                if (!(stepObj instanceof Map<?, ?> stepRaw)) {
                    continue;
                }
                Map<String, Object> step = (Map<String, Object>) stepRaw;
                String stepTitle = str(step.get("title"));
                if (!StringUtils.hasText(stepTitle)) {
                    continue;
                }
                WbTask child = new WbTask();
                child.setUserId(userId);
                child.setProjectId(projectId);
                child.setParentTaskId(parent.getTaskId());
                child.setPlanBatchId(planBatchId.trim());
                child.setTitle(stepTitle);
                child.setDescription(str(step.get("description")));
                child.setDueTime(toDate(step.get("dueTime")));
                child.setPriority(toInt(step.get("priority")));
                String status = str(step.get("status"));
                if (!List.of("0", "1", "2", "3").contains(status)) {
                    status = "0";
                }
                child.setStatus(status);
                child.setDisplayOrder(stepOrder++);
                child.setRemark("AI规划·步骤");
                save(child);
                created++;
            }
        }
        if (created == 0) {
            throw new RuntimeException("没有可写入的有效阶段或步骤");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("planBatchId", planBatchId.trim());
        result.put("projectId", projectId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int revokeByPlanBatchId(Long userId, String planBatchId) {
        if (!StringUtils.hasText(planBatchId)) {
            throw new RuntimeException("规划批次号不能为空");
        }
        List<WbTask> batch = list(
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getUserId, userId)
                        .eq(WbTask::getPlanBatchId, planBatchId.trim())
        );
        if (batch.isEmpty()) {
            return 0;
        }
        List<Long> ids = batch.stream().map(WbTask::getTaskId).toList();
        remove(
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getUserId, userId)
                        .in(WbTask::getTaskId, ids)
        );
        return ids.size();
    }

    private WbProject requireOwnedProject(Long userId, Long projectId) {
        if (projectId == null) {
            throw new RuntimeException("项目 ID 不能为空");
        }
        WbProject project = wbProjectService.getOne(
                new LambdaQueryWrapper<WbProject>()
                        .eq(WbProject::getProjectId, projectId)
                        .eq(WbProject::getUserId, userId)
        );
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }
        return project;
    }

    private WbTask requireOwnedTask(Long userId, Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("任务 ID 不能为空");
        }
        WbTask task = getOne(
                new LambdaQueryWrapper<WbTask>()
                        .eq(WbTask::getTaskId, taskId)
                        .eq(WbTask::getUserId, userId)
        );
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    private void assertAttachmentsWritable(Long userId, WbTask task) {
        if (task.getProjectId() == null) {
            return;
        }
        WbProject project = wbProjectService.getOne(
                new LambdaQueryWrapper<WbProject>()
                        .eq(WbProject::getProjectId, task.getProjectId())
                        .eq(WbProject::getUserId, userId)
        );
        if (project != null && "1".equals(String.valueOf(project.getStatus()))) {
            throw new RuntimeException("项目已归档，附件只读");
        }
    }

    private List<Map<String, Object>> parseAttachments(String json) {
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

    private String writeAttachments(List<Map<String, Object>> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            throw new RuntimeException("附件数据序列化失败");
        }
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Date toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date d) {
            return d;
        }
        String raw = String.valueOf(value).trim();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            if (raw.length() >= 19) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(raw.substring(0, 19));
            }
            if (raw.length() >= 10) {
                return new SimpleDateFormat("yyyy-MM-dd").parse(raw.substring(0, 10));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
