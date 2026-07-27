package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 客服消息 */
@Data
@TableName("cs_message")
public class CsMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    /** 1会员 2客服 */
    private Integer senderType;
    private Long senderId;
    /** 1文本 2图片 3文件 */
    private Integer contentType;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}
