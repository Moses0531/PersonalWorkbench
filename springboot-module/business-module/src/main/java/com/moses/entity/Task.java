package com.moses.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 业务-任务表（项目根 / 阶段 / 任务统一树）
 * @TableName task
 */
@TableName(value = "task")
@Data
public class Task {
    public static final String NODE_PROJECT = "project";
    public static final String NODE_PHASE = "phase";
    public static final String NODE_TASK = "task";

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long taskId;

    /**
     * 所属用户ID（逻辑关联 sys_user.user_id）
     */
    private Long userId;

    /**
     * 节点类型：project / phase / task
     */
    private String nodeType;

    /**
     * 是否归档（仅 project：0-否，1-是）
     */
    private String archived;

    /**
     * 父任务ID（逻辑关联 task.task_id；项目根为空）
     */
    private Long parentTaskId;

    /**
     * AI规划批次号（同批落板）
     */
    private String planBatchId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态/看板列（0-待办，1-进行中，2-已完成，3-已取消）
     */
    private String status;

    /**
     * 优先级（整数，用户自填）
     */
    private Integer priority;

    /**
     * 截止时间
     */
    private Date dueTime;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 同级显示顺序（越小越靠前）
     */
    private Integer displayOrder;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 附件列表（JSON 数组字符串）
     */
    private String attachments;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
