package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 收入管理列表项(支付流水联表) */
@Data
public class IncomeVO {
    private Long id;
    private String payNo;
    private Long bookingId;
    private Long memberId;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer payStatus;
    private LocalDateTime payTime;
    private String transactionNo;
    private LocalDateTime createTime;
    private String orderNo;
    private String roomName;
    private String memberName;
    private String memberUsername;
}
