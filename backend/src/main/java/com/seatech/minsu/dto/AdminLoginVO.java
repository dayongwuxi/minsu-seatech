package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class AdminLoginVO {
    private String token;
    private AdminVO admin;
}
