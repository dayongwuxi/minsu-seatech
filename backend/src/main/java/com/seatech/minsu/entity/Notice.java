package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 公告 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公告编号 GG+yyyyMMdd+4位随机 */
    private String noticeNo;
    private String title;
    private String summary;
    private String coverImage;
    /** 正文(富文本) */
    private String content;
    /** 0草稿/已下架 1已发布 */
    private Integer publishStatus;
    private LocalDateTime publishTime;
    private Long creatorId;
    private String creatorName;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
