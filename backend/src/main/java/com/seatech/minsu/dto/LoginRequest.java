package com.seatech.minsu.dto;

import lombok.Data;

/** account 支持用户名或手机号 */
@Data
public class LoginRequest {
    private String account;
    private String password;
    private String captchaKey;
    private String captchaCode;
}
