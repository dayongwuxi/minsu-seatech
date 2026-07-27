package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 会员视图：国家区号透传（脱敏只作用于号码本体，区号可见） */
class MemberVOTest {

    private Member member(String phone, String country) {
        Member m = new Member();
        m.setId(1L);
        m.setUsername("taro");
        m.setPhone(phone);
        m.setPhoneCountry(country);
        return m;
    }

    @Test
    @DisplayName("区号随 VO 透传（管理端展示完整号码）")
    void passesThroughPhoneCountry() {
        MemberVO vo = MemberVO.of(member("08012345678", "+81"), null, false);
        assertEquals("+81", vo.getPhoneCountry());
        assertEquals("08012345678", vo.getPhone());
    }

    @Test
    @DisplayName("脱敏模式下号码打码但区号保留")
    void maskKeepsCountryCode() {
        MemberVO vo = MemberVO.of(member("08012345678", "+81"), null, true);
        assertEquals("+81", vo.getPhoneCountry());
        assertEquals("080****5678", vo.getPhone());
    }

    @Test
    @DisplayName("存量用户区号为空")
    void nullCountryStaysNull() {
        assertNull(MemberVO.of(member("13800138000", null), null, false).getPhoneCountry());
    }
}
