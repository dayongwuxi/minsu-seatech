package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 客服会话(含会员昵称) */
@Data
public class CsSessionVO {
    private Long id;
    private Long memberId;
    private Long adminId;
    private String sourceChannel;
    private Integer status;
    private String lastMessage;
    private LocalDateTime lastTime;
    private Integer unreadAdmin;
    private Integer unreadMember;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String memberName;
    private String memberAvatar;
}
