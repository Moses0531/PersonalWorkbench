package com.moses.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 业务-笔记
 * @TableName note
 */
@TableName(value = "note")
@Data
public class Note {
    public static final String TYPE_SCRAP = "scrap";
    public static final String TYPE_DIGEST = "digest";
    public static final String STATUS_ACTIVE = "0";
    public static final String STATUS_ARCHIVED = "1";

    /**
     * 笔记ID
     */
    @TableId(type = IdType.AUTO)
    private Long noteId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 标题（碎片可空）
     */
    private String title;

    /**
     * 正文（纯文本/Markdown）
     */
    private String content;

    /**
     * 类型：scrap / digest
     */
    private String noteType;

    /**
     * 0-活跃，1-已归档
     */
    private String status;

    /**
     * 整理稿来源碎片 ID 列表（JSON 数组字符串）
     */
    private String sourceNoteIds;

    /**
     * 生成任务后挂接的顶层任务 ID
     */
    private Long linkedTaskId;

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
