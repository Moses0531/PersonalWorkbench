package com.moses.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 工作台-任务表（独立任务或项目子任务）
 * @TableName wb_task
 */
@TableName(value ="wb_task")
@Data
public class WbTask {
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
     * 所属项目ID（空=独立任务；逻辑关联 wb_project.project_id）
     */
    private Long projectId;

    /**
     * 父任务ID（逻辑关联 wb_task.task_id，可选子步骤）
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
     * 同状态下显示顺序（越小越靠前）
     */
    private Integer displayOrder;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 附件列表（JSON 数组字符串，挂本任务；元素含 id/name/url/size/mime/createTime）
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

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        WbTask other = (WbTask) that;
        return (this.getTaskId() == null ? other.getTaskId() == null : this.getTaskId().equals(other.getTaskId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getProjectId() == null ? other.getProjectId() == null : this.getProjectId().equals(other.getProjectId()))
            && (this.getParentTaskId() == null ? other.getParentTaskId() == null : this.getParentTaskId().equals(other.getParentTaskId()))
            && (this.getPlanBatchId() == null ? other.getPlanBatchId() == null : this.getPlanBatchId().equals(other.getPlanBatchId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getPriority() == null ? other.getPriority() == null : this.getPriority().equals(other.getPriority()))
            && (this.getDueTime() == null ? other.getDueTime() == null : this.getDueTime().equals(other.getDueTime()))
            && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()))
            && (this.getDisplayOrder() == null ? other.getDisplayOrder() == null : this.getDisplayOrder().equals(other.getDisplayOrder()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getTaskId() == null) ? 0 : getTaskId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getProjectId() == null) ? 0 : getProjectId().hashCode());
        result = prime * result + ((getParentTaskId() == null) ? 0 : getParentTaskId().hashCode());
        result = prime * result + ((getPlanBatchId() == null) ? 0 : getPlanBatchId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getPriority() == null) ? 0 : getPriority().hashCode());
        result = prime * result + ((getDueTime() == null) ? 0 : getDueTime().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        result = prime * result + ((getDisplayOrder() == null) ? 0 : getDisplayOrder().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", taskId=").append(taskId);
        sb.append(", userId=").append(userId);
        sb.append(", projectId=").append(projectId);
        sb.append(", parentTaskId=").append(parentTaskId);
        sb.append(", planBatchId=").append(planBatchId);
        sb.append(", title=").append(title);
        sb.append(", description=").append(description);
        sb.append(", status=").append(status);
        sb.append(", priority=").append(priority);
        sb.append(", dueTime=").append(dueTime);
        sb.append(", tags=").append(tags);
        sb.append(", displayOrder=").append(displayOrder);
        sb.append(", remark=").append(remark);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}