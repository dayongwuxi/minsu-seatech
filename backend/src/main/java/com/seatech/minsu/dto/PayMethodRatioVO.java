package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 支付方式占比 */
@Data
public class PayMethodRatioVO {
    private Integer payMethod;
    private Long count;
    private BigDecimal amount;
}
