package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 前台会员 */
@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会员编号 M+yyyyMMdd+4位随机 */
    private String memberNo;
    private String username;
    /** 手机号：纯数字，不限位数 */
    private String phone;
    /** 国家区号，形如 +81/+86/+1 */
    private String phoneCountry;
    /** BCrypt加密 */
    private String password;
    private String name;
    private String email;
    private String avatar;
    private Long memberTypeId;
    /** Stripe Customer id */
    private String stripeCustomerId;
    /** 0禁用 1正常 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;
}
