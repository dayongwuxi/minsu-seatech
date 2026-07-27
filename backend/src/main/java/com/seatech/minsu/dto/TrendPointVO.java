package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 趋势图数据点 */
@Data
public class TrendPointVO {
    private String date;
    private BigDecimal value;
}
