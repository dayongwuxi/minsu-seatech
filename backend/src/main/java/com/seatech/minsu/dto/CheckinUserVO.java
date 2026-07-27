package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 会员端入住记录项 */
@Data
public class CheckinUserVO {
    private Long id;
    private String checkinNo;
    private Long bookingId;
    private Long roomId;
    private String guestName;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private Integer status;
    private Integer isReviewed;
    private LocalDateTime createTime;
    private String roomName;
    private String roomCover;
    private String orderNo;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
}
