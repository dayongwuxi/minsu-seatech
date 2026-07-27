package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 支付结果画面所需字段 */
@Data
public class PaymentResultVO {
    private String orderNo;
    private String payNo;
    private BigDecimal amount;
    private String currency;
    private Integer payMethod;
    /** 1模拟 2Stripe */
    private Integer channel;
    private Integer payStatus;
    private LocalDateTime payTime;
    private String roomName;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer nights;
}
