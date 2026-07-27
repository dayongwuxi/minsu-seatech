package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    /** 手机号：纯数字，不限位数（支持各国制式） */
    private String phone;
    /** 国家区号，形如 +81/+86/+1（可选） */
    private String phoneCountry;
    private String password;
    private String confirmPassword;
    private String captchaKey;
    private String captchaCode;
}
