package com.moses.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.entity.WbTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @description 针对表【wb_task(工作台-任务表（项目根/阶段/任务统一树）)】的数据库操作Service
 * @createDate 2026-07-16 22:59:29
 */
public interface WbTaskService extends IService<WbTask> {

    void createOwnedTask(Long userId, WbTask task);

    void updateOwnedTask(Long userId, WbTask task);

    void deleteOwnedTasksCascade(Long userId, List<Long> ids);

    void setProjectArchived(Long userId, Long projectTaskId, String archived);

    Map<String, Object> uploadAttachment(Long userId, Long taskId, MultipartFile file);

    void removeAttachment(Long userId, Long taskId, String attachmentId);

    /**
     * 更新附件处理状态（与任务看板 status 无关）
     * status: 0-未处理，1-已处理，2-处理完成
     */
    Map<String, Object> updateAttachmentStatus(Long userId, Long taskId, String attachmentId, String status);

    /** @param projectTaskId 项目根任务 ID */
    List<Map<String, Object>> listProjectAttachments(Long userId, Long projectTaskId);

    Map<String, Object> getProjectBoardSnapshot(Long userId, Long projectTaskId);

    /** phases: [{title, description, steps:[{title, description, priority, status}]}] */
    Map<String, Object> applyPlanBatch(Long userId, Long projectTaskId, String planBatchId,
                                       List<Map<String, Object>> phases);
}
