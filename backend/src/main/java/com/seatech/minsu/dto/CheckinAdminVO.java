package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端入住记录项 */
@Data
public class CheckinAdminVO {
    private Long id;
    private String checkinNo;
    private Long bookingId;
    private Long memberId;
    private Long roomId;
    private String guestName;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private Integer status;
    private Integer isReviewed;
    private LocalDateTime createTime;
    private String roomName;
    private String roomNo;
    private String orderNo;
    private String memberName;
    private String memberUsername;
}
