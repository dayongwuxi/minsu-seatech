package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String username;
    private String password;
    private String captchaKey;
    private String captchaCode;
}
