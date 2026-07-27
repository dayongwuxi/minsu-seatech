package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RateUpdateRequest {
    private BigDecimal rate;
}
