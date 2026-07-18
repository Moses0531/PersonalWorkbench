package com.moses.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 工作台-会议记录表
 * @TableName wb_meeting
 */
@TableName(value = "wb_meeting")
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
     * 会议材料（JSON 数组字符串；元素含 id/name/url/size/mime/createTime）
     */
    private String attachments;

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
