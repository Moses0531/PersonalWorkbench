package com.moses.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moses.entity.WbTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @description 针对表【wb_task(工作台-任务表（独立任务或项目子任务）)】的数据库操作Service
 * @createDate 2026-07-16 22:59:29
 */
public interface WbTaskService extends IService<WbTask> {

    Map<String, Object> uploadAttachment(Long userId, Long taskId, MultipartFile file);

    void removeAttachment(Long userId, Long taskId, String attachmentId);

    List<Map<String, Object>> listProjectAttachments(Long userId, Long projectId);
}
