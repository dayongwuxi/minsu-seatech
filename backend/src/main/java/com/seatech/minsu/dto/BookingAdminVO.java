package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 管理端预定列表项(含会员/房间名称) */
@Data
public class BookingAdminVO {
    private Long id;
    private String orderNo;
    private Long memberId;
    private Long roomId;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer nights;
    private Integer guestCount;
    private String guestName;
    private String contactPhone;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer payStatus;
    private Integer bookingStatus;
    private String userRemark;
    private String adminRemark;
    private LocalDateTime createTime;
    private String memberName;
    private String memberUsername;
    private String memberPhone;
    private String roomName;
    private String roomNo;
}
