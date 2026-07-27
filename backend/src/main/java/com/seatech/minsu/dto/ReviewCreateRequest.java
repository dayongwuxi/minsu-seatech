package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewCreateRequest {
    private Long bookingId;
    /** 0.5~5.0 支持半星 */
    private BigDecimal rating;
    private String content;
}
