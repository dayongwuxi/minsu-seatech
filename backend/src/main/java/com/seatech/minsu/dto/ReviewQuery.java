package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 管理端评价分页查询参数 */
@Data
public class ReviewQuery {
    private long current = 1;
    private long size = 10;
    private String roomName;
    private String memberName;
    private BigDecimal rating;
    private Integer auditStatus;
}
