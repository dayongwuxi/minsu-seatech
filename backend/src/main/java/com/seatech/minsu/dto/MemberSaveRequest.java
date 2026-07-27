package com.seatech.minsu.dto;

import lombok.Data;

/** 管理端会员新增/修改请求 */
@Data
public class MemberSaveRequest {
    private String username;
    private String phone;
    /** 为空则新增时用默认密码 123456 / 修改时保持原密码 */
    private String password;
    private String name;
    private String email;
    private String avatar;
    private Long memberTypeId;
    private Integer status;
}
