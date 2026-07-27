package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端会员列表项(含类型名称) */
@Data
public class MemberAdminVO {
    private Long id;
    private String memberNo;
    private String username;
    private String phone;
    /** 国家区号(+81 等) */
    private String phoneCountry;
    private String name;
    private String email;
    private String avatar;
    private Long memberTypeId;
    private String memberTypeName;
    private Integer status;
    private LocalDateTime registerTime;
}
