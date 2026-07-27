package com.seatech.minsu.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 管理端预定分页查询参数 */
@Data
public class BookingQuery {
    private long current = 1;
    private long size = 10;
    private String orderNo;
    private String memberName;
    private String roomName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinEnd;
    private Integer payStatus;
    private Integer bookingStatus;
}
