package com.moses.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 工作台-会议记录表
 * @TableName wb_meeting
 */
@TableName(value ="wb_meeting")
@Data
public class WbMeeting {
    /**
     * 会议ID
     */
    @TableId(type = IdType.AUTO)
    private Long meetingId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 会议标题
     */
    private String title;

    /**
     * 会议时间
     */
    private Date meetingTime;

    /**
     * 地点（可选）
     */
    private String location;

    /**
     * 参会人（可选）
     */
    private String participants;

    /**
     * 会议材料（OSS；元素含 id/name/url/size/mime/createTime）
     */
    private Object attachments;

    /**
     * AI整理后的会议概要
     */
    private String aiSummary;

    /**
     * 0-待整理，1-已整理
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
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
        WbMeeting other = (WbMeeting) that;
        return (this.getMeetingId() == null ? other.getMeetingId() == null : this.getMeetingId().equals(other.getMeetingId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getMeetingTime() == null ? other.getMeetingTime() == null : this.getMeetingTime().equals(other.getMeetingTime()))
            && (this.getLocation() == null ? other.getLocation() == null : this.getLocation().equals(other.getLocation()))
            && (this.getParticipants() == null ? other.getParticipants() == null : this.getParticipants().equals(other.getParticipants()))
            && (this.getAttachments() == null ? other.getAttachments() == null : this.getAttachments().equals(other.getAttachments()))
            && (this.getAiSummary() == null ? other.getAiSummary() == null : this.getAiSummary().equals(other.getAiSummary()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getMeetingId() == null) ? 0 : getMeetingId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getMeetingTime() == null) ? 0 : getMeetingTime().hashCode());
        result = prime * result + ((getLocation() == null) ? 0 : getLocation().hashCode());
        result = prime * result + ((getParticipants() == null) ? 0 : getParticipants().hashCode());
        result = prime * result + ((getAttachments() == null) ? 0 : getAttachments().hashCode());
        result = prime * result + ((getAiSummary() == null) ? 0 : getAiSummary().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
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
        sb.append(", meetingId=").append(meetingId);
        sb.append(", userId=").append(userId);
        sb.append(", title=").append(title);
        sb.append(", meetingTime=").append(meetingTime);
        sb.append(", location=").append(location);
        sb.append(", participants=").append(participants);
        sb.append(", attachments=").append(attachments);
        sb.append(", aiSummary=").append(aiSummary);
        sb.append(", status=").append(status);
        sb.append(", remark=").append(remark);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}