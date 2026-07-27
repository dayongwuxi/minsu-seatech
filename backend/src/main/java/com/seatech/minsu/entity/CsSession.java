package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 客服会话 */
@Data
@TableName("cs_session")
public class CsSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    /** 接入客服(admin表) */
    private Long adminId;
    /** 来源渠道: web/微信小程序 */
    private String sourceChannel;
    /** 0排队中 1会话中 2已结束 */
    private Integer status;
    private String lastMessage;
    private LocalDateTime lastTime;
    /** 客服未读数 */
    private Integer unreadAdmin;
    /** 用户未读数 */
    private Integer unreadMember;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
