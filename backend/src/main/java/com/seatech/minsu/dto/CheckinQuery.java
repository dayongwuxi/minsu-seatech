package com.seatech.minsu.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 管理端入住记录分页查询参数 */
@Data
public class CheckinQuery {
    private long current = 1;
    private long size = 10;
    private String guestName;
    private String roomName;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateEnd;
    private Integer status;
}
