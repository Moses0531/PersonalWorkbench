package com.moses.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moses.config.ResultConfig;
import com.moses.entity.Task;
import com.moses.mapper.TaskMapper;
import com.moses.service.TaskService;
import com.moses.utils.UploadFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * @author Administrator
 * @description 针对表【task(工作台-任务表（项目根/阶段/任务统一树）)】的数据库操作Service实现
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task>
        implements TaskService {

    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;
    private static final int MAX_ATTACHMENT_COUNT = 20;

    @Autowired
    private UploadFileUtil uploadFileUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOwnedTask(Long userId, Task task) {
        task.setTaskId(null);
        task.setUserId(userId);
        task.setAttachments(null);
        normalizeAndValidate(userId, task, true);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOwnedTask(Long userId, Task task) {
        if (task.getTaskId() == null) {
            throw new RuntimeException("任务 ID 不能为空");
        }
        Task existing = requireOwnedTask(userId, task.getTaskId());
        task.setUserId(userId);
        task.setAttachments(null);
        if (!StringUtils.hasText(task.getNodeType())) {
            task.setNodeType(existing.getNodeType());
        }
        if (task.getArchived() == null) {
            task.setArchived(existing.getArchived());
        }
        normalizeAndValidate(userId, task, false);
        task.setUpdateTime(new Date());
        update(
                task,
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getTaskId, task.getTaskId())
                        .eq(Task::getUserId, userId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedTasksCascade(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<Long> toDelete = new HashSet<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            Task root = getOne(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getTaskId, id)
                            .eq(Task::getUserId, userId)
            );
            if (root == null) {
                continue;
            }
            toDelete.add(id);
            toDelete.addAll(collectDescendantIds(userId, id));
        }
        if (toDelete.isEmpty()) {
            return;
        }
        remove(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .in(Task::getTaskId, toDelete)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setProjectArchived(Long userId, Long projectTaskId, String archived) {
        Task project = requireOwnedProject(userId, projectTaskId);
        String flag = "1".equals(String.valueOf(archived).trim()) ? "1" : "0";
        project.setArchived(flag);
        project.setUpdateTime(new Date());
        updateById(project);
    }

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

        Task task = requireOwnedTask(userId, taskId);
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
            item.put("status", "0");

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
        Task task = requireOwnedTask(userId, taskId);
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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateAttachmentStatus(Long userId, Long taskId, String attachmentId, String status) {
        if (!StringUtils.hasText(attachmentId)) {
            throw new RuntimeException("附件 ID 不能为空");
        }
        String normalized = normalizeAttachmentStatus(status);
        Task task = requireOwnedTask(userId, taskId);
        assertAttachmentsWritable(userId, task);

        List<Map<String, Object>> list = parseAttachments(task.getAttachments());
        Map<String, Object> target = null;
        for (Map<String, Object> item : list) {
            if (attachmentId.equals(String.valueOf(item.get("id")))) {
                target = item;
                break;
            }
        }
        if (target == null) {
            throw new RuntimeException("附件不存在");
        }
        target.put("status", normalized);
        target.remove("handled");
        task.setAttachments(writeAttachments(list));
        task.setUpdateTime(new Date());
        updateById(task);
        return target;
    }

    @Override
    public List<Map<String, Object>> listProjectAttachments(Long userId, Long projectTaskId) {
        requireOwnedProject(userId, projectTaskId);
        List<Task> tasks = listDescendants(userId, projectTaskId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            for (Map<String, Object> item : parseAttachments(task.getAttachments())) {
                Map<String, Object> row = new HashMap<>(item);
                row.put("status", resolveAttachmentStatus(item));
                row.remove("handled");
                row.put("taskId", task.getTaskId());
                row.put("taskTitle", task.getTitle());
                result.add(row);
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getProjectBoardSnapshot(Long userId, Long projectTaskId) {
        Task project = requireOwnedProject(userId, projectTaskId);
        List<Task> tasks = listDescendants(userId, projectTaskId);

        List<Map<String, Object>> taskSummaries = new ArrayList<>();
        int todo = 0;
        int doing = 0;
        int done = 0;
        int cancelled = 0;
        for (Task task : tasks) {
            String status = task.getStatus() != null ? String.valueOf(task.getStatus()) : "0";
            if (Task.NODE_TASK.equals(normalizeNodeType(task.getNodeType()))) {
                switch (status) {
                    case "1" -> doing++;
                    case "2" -> done++;
                    case "3" -> cancelled++;
                    default -> todo++;
                }
            }
            Map<String, Object> row = new HashMap<>();
            row.put("taskId", task.getTaskId());
            row.put("parentTaskId", task.getParentTaskId());
            row.put("nodeType", normalizeNodeType(task.getNodeType()));
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
        snapshot.put("projectTaskId", project.getTaskId());
        snapshot.put("name", project.getTitle());
        snapshot.put("description", project.getDescription());
        snapshot.put("archived", project.getArchived() != null ? project.getArchived() : "0");
        snapshot.put("counts", counts);
        snapshot.put("tasks", taskSummaries);
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public Map<String, Object> applyPlanBatch(Long userId, Long projectTaskId, String planBatchId,
                                              List<Map<String, Object>> phases) {
        if (!StringUtils.hasText(planBatchId)) {
            throw new RuntimeException("规划批次号不能为空");
        }
        if (phases == null || phases.isEmpty()) {
            throw new RuntimeException("规划阶段不能为空");
        }
        Task project = requireOwnedProject(userId, projectTaskId);
        if ("1".equals(String.valueOf(project.getArchived()))) {
            throw new RuntimeException("已归档，无法写入规划任务");
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
            Task parent = new Task();
            parent.setUserId(userId);
            parent.setNodeType(Task.NODE_PHASE);
            parent.setArchived("0");
            parent.setParentTaskId(projectTaskId);
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
                Task child = new Task();
                child.setUserId(userId);
                child.setNodeType(Task.NODE_TASK);
                child.setArchived("0");
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
        result.put("projectTaskId", projectTaskId);
        return result;
    }

    private void normalizeAndValidate(Long userId, Task task, boolean creating) {
        if (!StringUtils.hasText(task.getTitle())) {
            throw new RuntimeException("标题不能为空");
        }
        String nodeType = normalizeNodeType(task.getNodeType());
        task.setNodeType(nodeType);
        if (task.getArchived() == null || !StringUtils.hasText(task.getArchived())) {
            task.setArchived("0");
        }
        if (!"0".equals(task.getArchived()) && !"1".equals(task.getArchived())) {
            throw new RuntimeException("归档标记无效");
        }
        if (!Task.NODE_PROJECT.equals(nodeType) && "1".equals(task.getArchived())) {
            throw new RuntimeException("仅顶层任务可设置归档");
        }
        if (task.getStatus() == null || !StringUtils.hasText(task.getStatus())) {
            task.setStatus("0");
        }
        if (task.getPriority() == null) {
            task.setPriority(0);
        }
        if (task.getDisplayOrder() == null) {
            task.setDisplayOrder(0);
        }
        if (task.getDescription() == null) {
            task.setDescription("");
        }
        if (task.getTags() == null) {
            task.setTags("");
        }
        if (task.getRemark() == null) {
            task.setRemark("");
        }

        Long parentId = task.getParentTaskId();
        if (Task.NODE_PROJECT.equals(nodeType)) {
            if (parentId != null) {
                throw new RuntimeException("顶层任务不能挂在其它节点下");
            }
            return;
        }
        if (parentId == null) {
            if (!Task.NODE_TASK.equals(nodeType)) {
                throw new RuntimeException("阶段必须挂在顶层任务下");
            }
            return;
        }
        if (!creating && Objects.equals(parentId, task.getTaskId())) {
            throw new RuntimeException("不能将自身设为父任务");
        }
        Task parent = requireOwnedTask(userId, parentId);
        String parentType = normalizeNodeType(parent.getNodeType());
        if (Task.NODE_PHASE.equals(nodeType)) {
            if (!Task.NODE_PROJECT.equals(parentType)) {
                throw new RuntimeException("阶段的父节点必须是顶层任务");
            }
        } else if (Task.NODE_TASK.equals(nodeType)) {
            if (!Task.NODE_PROJECT.equals(parentType) && !Task.NODE_PHASE.equals(parentType)) {
                throw new RuntimeException("任务只能挂在顶层任务或阶段下");
            }
        } else {
            throw new RuntimeException("节点类型无效");
        }
    }

    private List<Long> collectDescendantIds(Long userId, Long rootId) {
        List<Long> ids = new ArrayList<>();
        for (Task t : listDescendants(userId, rootId)) {
            ids.add(t.getTaskId());
        }
        return ids;
    }

    private List<Task> listDescendants(Long userId, Long rootId) {
        List<Task> all = list(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .orderByAsc(Task::getDisplayOrder)
                        .orderByDesc(Task::getUpdateTime)
        );
        Map<Long, List<Task>> children = new HashMap<>();
        for (Task t : all) {
            if (t.getParentTaskId() == null) {
                continue;
            }
            children.computeIfAbsent(t.getParentTaskId(), k -> new ArrayList<>()).add(t);
        }
        List<Task> result = new ArrayList<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        Set<Long> visited = new HashSet<>();
        visited.add(rootId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            List<Task> kids = children.getOrDefault(pid, List.of());
            for (Task kid : kids) {
                if (!visited.add(kid.getTaskId())) {
                    continue;
                }
                result.add(kid);
                queue.add(kid.getTaskId());
            }
        }
        return result;
    }

    private Task requireOwnedProject(Long userId, Long projectTaskId) {
        Task project = requireOwnedTask(userId, projectTaskId);
        if (!Task.NODE_PROJECT.equals(normalizeNodeType(project.getNodeType()))) {
            throw new RuntimeException("顶层任务不存在");
        }
        return project;
    }

    private Task requireOwnedTask(Long userId, Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("任务 ID 不能为空");
        }
        Task task = getOne(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getTaskId, taskId)
                        .eq(Task::getUserId, userId)
        );
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    private void assertAttachmentsWritable(Long userId, Task task) {
        Task project = findProjectRoot(userId, task);
        if (project != null && "1".equals(String.valueOf(project.getArchived()))) {
            throw new RuntimeException("已归档，附件只读");
        }
    }

    private Task findProjectRoot(Long userId, Task task) {
        if (task == null) {
            return null;
        }
        if (Task.NODE_PROJECT.equals(normalizeNodeType(task.getNodeType()))) {
            return task;
        }
        Long cursor = task.getParentTaskId();
        Set<Long> seen = new HashSet<>();
        while (cursor != null && seen.add(cursor)) {
            Task node = getOne(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getTaskId, cursor)
                            .eq(Task::getUserId, userId)
            );
            if (node == null) {
                return null;
            }
            if (Task.NODE_PROJECT.equals(normalizeNodeType(node.getNodeType()))) {
                return node;
            }
            cursor = node.getParentTaskId();
        }
        return null;
    }

    private static String normalizeNodeType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return Task.NODE_TASK;
        }
        String raw = nodeType.trim().toLowerCase();
        if (Task.NODE_PROJECT.equals(raw) || Task.NODE_PHASE.equals(raw) || Task.NODE_TASK.equals(raw)) {
            return raw;
        }
        throw new RuntimeException("节点类型无效，仅支持 project / phase / task");
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

    private static String normalizeAttachmentStatus(String status) {
        String raw = status == null ? "" : status.trim();
        if ("0".equals(raw) || "1".equals(raw) || "2".equals(raw)) {
            return raw;
        }
        throw new RuntimeException("附件状态无效，仅支持 0-未处理 / 1-已处理 / 2-处理完成");
    }

    private static String resolveAttachmentStatus(Map<String, Object> item) {
        if (item == null) {
            return "0";
        }
        Object status = item.get("status");
        if (status != null) {
            String raw = String.valueOf(status).trim();
            if ("0".equals(raw) || "1".equals(raw) || "2".equals(raw)) {
                return raw;
            }
        }
        Object handled = item.get("handled");
        if (handled instanceof Boolean b) {
            return b ? "1" : "0";
        }
        if (handled != null) {
            String raw = String.valueOf(handled).trim();
            if ("true".equalsIgnoreCase(raw) || "1".equals(raw)) {
                return "1";
            }
        }
        return "0";
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
