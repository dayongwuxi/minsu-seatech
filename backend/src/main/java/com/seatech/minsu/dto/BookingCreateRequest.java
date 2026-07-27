package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingCreateRequest {
    private Long roomId;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer guestCount;
    private String guestName;
    private String contactPhone;
    private String userRemark;
    /** 优惠码(可选) */
    private String promoCode;
}
