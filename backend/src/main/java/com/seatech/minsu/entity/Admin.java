package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台管理员(客服角色亦在此表) */
@Data
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** BCrypt加密 */
    private String password;
    private String name;
    private String phone;
    private String email;
    private String avatar;
    private Long roleId;
    /** 0禁用 1启用 */
    private Integer status;
    private LocalDateTime lastLoginTime;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
