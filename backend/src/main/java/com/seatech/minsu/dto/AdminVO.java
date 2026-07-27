package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Admin;
import lombok.Data;

import java.time.LocalDateTime;

/** 管理员视图(不含密码) */
@Data
public class AdminVO {

    private Long id;
    private String username;
    private String name;
    private String phone;
    private String email;
    private String avatar;
    private Long roleId;
    private String roleName;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    public static AdminVO of(Admin a, String roleName) {
        AdminVO vo = new AdminVO();
        vo.setId(a.getId());
        vo.setUsername(a.getUsername());
        vo.setName(a.getName());
        vo.setPhone(a.getPhone());
        vo.setEmail(a.getEmail());
        vo.setAvatar(a.getAvatar());
        vo.setRoleId(a.getRoleId());
        vo.setRoleName(roleName);
        vo.setStatus(a.getStatus());
        vo.setLastLoginTime(a.getLastLoginTime());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }
}
