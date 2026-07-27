package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 公开汇率项 */
@Data
public class RateItemVO {
    private String code;
    private String name;
    private String symbol;
    private BigDecimal rate;
    private LocalDateTime updateTime;
}
