package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 收入统计：今日/本月/累计/退款/实际收入 + 环比增减 */
@Data
public class IncomeStatsVO {
    private BigDecimal todayIncome;
    /** 较昨日增减额 */
    private BigDecimal todayDiff;
    private BigDecimal monthIncome;
    /** 较上月增减额 */
    private BigDecimal monthDiff;
    private BigDecimal totalIncome;
    private BigDecimal refundTotal;
    private BigDecimal actualIncome;
}
