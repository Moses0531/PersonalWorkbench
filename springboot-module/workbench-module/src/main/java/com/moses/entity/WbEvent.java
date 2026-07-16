package com.moses.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 工作台-日程表
 * @TableName wb_event
 */
@TableName(value ="wb_event")
@Data
public class WbEvent {
    /**
     * 日程ID
     */
    @TableId(type = IdType.AUTO)
    private Long eventId;

    /**
     * 所属用户ID（逻辑关联 sys_user.user_id）
     */
    private Long userId;

    /**
     * 关联任务ID（可选；逻辑关联 wb_task.task_id）
     */
    private Long taskId;

    /**
     * 标题
     */
    private String title;

    /**
     * 地点
     */
    private String location;

    /**
     * 开始时间（重复日程表示当日时段起点）
     */
    private Date startTime;

    /**
     * 结束时间（重复日程表示当日时段终点）
     */
    private Date endTime;

    /**
     * 是否全天（0-否，1-是）
     */
    private Integer isAllDay;

    /**
     * 重复类型（0-不重复，1-每周）
     */
    private String repeatType;

    /**
     * 每周重复的星期（1=周一…7=周日，逗号分隔，如1或1,3,5）
     */
    private String repeatWeekdays;

    /**
     * 同日显示顺序（越小越靠前）
     */
    private Integer displayOrder;

    /**
     * 备注说明
     */
    private String remark;

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
        WbEvent other = (WbEvent) that;
        return (this.getEventId() == null ? other.getEventId() == null : this.getEventId().equals(other.getEventId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getTaskId() == null ? other.getTaskId() == null : this.getTaskId().equals(other.getTaskId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getLocation() == null ? other.getLocation() == null : this.getLocation().equals(other.getLocation()))
            && (this.getStartTime() == null ? other.getStartTime() == null : this.getStartTime().equals(other.getStartTime()))
            && (this.getEndTime() == null ? other.getEndTime() == null : this.getEndTime().equals(other.getEndTime()))
            && (this.getIsAllDay() == null ? other.getIsAllDay() == null : this.getIsAllDay().equals(other.getIsAllDay()))
            && (this.getRepeatType() == null ? other.getRepeatType() == null : this.getRepeatType().equals(other.getRepeatType()))
            && (this.getRepeatWeekdays() == null ? other.getRepeatWeekdays() == null : this.getRepeatWeekdays().equals(other.getRepeatWeekdays()))
            && (this.getDisplayOrder() == null ? other.getDisplayOrder() == null : this.getDisplayOrder().equals(other.getDisplayOrder()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEventId() == null) ? 0 : getEventId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getTaskId() == null) ? 0 : getTaskId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getLocation() == null) ? 0 : getLocation().hashCode());
        result = prime * result + ((getStartTime() == null) ? 0 : getStartTime().hashCode());
        result = prime * result + ((getEndTime() == null) ? 0 : getEndTime().hashCode());
        result = prime * result + ((getIsAllDay() == null) ? 0 : getIsAllDay().hashCode());
        result = prime * result + ((getRepeatType() == null) ? 0 : getRepeatType().hashCode());
        result = prime * result + ((getRepeatWeekdays() == null) ? 0 : getRepeatWeekdays().hashCode());
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
        sb.append(", eventId=").append(eventId);
        sb.append(", userId=").append(userId);
        sb.append(", taskId=").append(taskId);
        sb.append(", title=").append(title);
        sb.append(", location=").append(location);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", isAllDay=").append(isAllDay);
        sb.append(", repeatType=").append(repeatType);
        sb.append(", repeatWeekdays=").append(repeatWeekdays);
        sb.append(", displayOrder=").append(displayOrder);
        sb.append(", remark=").append(remark);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}