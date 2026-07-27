package com.seatech.minsu.dto;

import lombok.Data;

import java.util.List;

/** 公开汇率表：展示换算用，结算一律以 base(CNY) */
@Data
public class ExchangeRatesVO {
    private String base;
    private List<RateItemVO> rates;
}
