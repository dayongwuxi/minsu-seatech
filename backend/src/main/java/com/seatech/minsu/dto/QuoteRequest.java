package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class QuoteRequest {
    private Long roomId;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private String promoCode;
}
