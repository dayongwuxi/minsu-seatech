package com.seatech.minsu.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 收入管理分页查询参数 */
@Data
public class IncomeQuery {
    private long current = 1;
    private long size = 10;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateEnd;
    private String roomName;
    private Integer payMethod;
    private Integer payStatus;
}
