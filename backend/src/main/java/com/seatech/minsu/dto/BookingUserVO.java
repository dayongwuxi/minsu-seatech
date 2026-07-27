package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 会员端预约列表项 */
@Data
public class BookingUserVO {
    private Long id;
    private String orderNo;
    private Long roomId;
    private String roomName;
    private String roomCover;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer nights;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private Integer payStatus;
    private Integer bookingStatus;
    /** 最新退款申请状态: 0申请中 1退款中 2已拒绝 3已退款, 无申请为 null */
    private Integer refundStatus;
    private LocalDateTime createTime;
}
