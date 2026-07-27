package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 数据看板核心指标(含较昨日增减) */
@Data
public class DashboardStatsVO {
    private Long todayBookings;
    private Long bookingsDiff;
    private Long todayCheckins;
    private Long checkinsDiff;
    private BigDecimal todayIncome;
    private BigDecimal incomeDiff;
    private Long pendingFeedbacks;
    private Long roomTotal;
    private Long availableRooms;
    private Long memberTotal;
    private Long bookingTotal;
}
