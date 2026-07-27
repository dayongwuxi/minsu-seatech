package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class CaptchaVO {
    private String key;
    private String imageBase64;
}
