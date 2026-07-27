package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Member;
import lombok.Data;

import java.time.LocalDateTime;

/** 会员视图（可选手机号/邮箱脱敏） */
@Data
public class MemberVO {

    private Long id;
    private String memberNo;
    private String username;
    private String phone;
    /** 国家区号(+81 等)；脱敏只作用于号码本体，区号可见 */
    private String phoneCountry;
    private String name;
    private String email;
    private String avatar;
    private Long memberTypeId;
    private String memberTypeName;
    private Integer status;
    private LocalDateTime registerTime;

    public static MemberVO of(Member m, String memberTypeName, boolean mask) {
        MemberVO vo = new MemberVO();
        vo.setId(m.getId());
        vo.setMemberNo(m.getMemberNo());
        vo.setUsername(m.getUsername());
        vo.setPhone(mask ? maskPhone(m.getPhone()) : m.getPhone());
        vo.setPhoneCountry(m.getPhoneCountry());
        vo.setName(m.getName());
        vo.setEmail(mask ? maskEmail(m.getEmail()) : m.getEmail());
        vo.setAvatar(m.getAvatar());
        vo.setMemberTypeId(m.getMemberTypeId());
        vo.setMemberTypeName(memberTypeName);
        vo.setStatus(m.getStatus());
        vo.setRegisterTime(m.getRegisterTime());
        return vo;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
