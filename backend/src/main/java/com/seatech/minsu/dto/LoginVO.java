package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private MemberVO member;
}
