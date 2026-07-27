package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.JwtUtil;
import com.seatech.minsu.dto.RegisterRequest;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.service.CaptchaService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.MemberTypeService;
import com.seatech.minsu.testutil.ChainMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册手机号国际化：不限位数仅要求纯数字（支持日本 080 等各国制式），
 * 附带国家区号（+81 等）单独存储，登录仍按注册时输入的号码数字精确匹配。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerTest {

    @Mock
    MemberService memberService;
    @Mock
    MemberTypeService memberTypeService;
    @Mock
    CaptchaService captchaService;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtUtil jwtUtil;

    LambdaQueryChainWrapper<Member> memberQuery;

    AuthController controller;

    @BeforeEach
    void setUp() {
        memberQuery = ChainMocks.queryChain();
        when(memberService.lambdaQuery()).thenReturn(memberQuery);
        when(memberQuery.count()).thenReturn(0L);
        when(captchaService.verify(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        controller = new AuthController(memberService, memberTypeService,
                captchaService, passwordEncoder, jwtUtil);
    }

    private RegisterRequest req(String phone, String phoneCountry) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("taro");
        r.setPhone(phone);
        r.setPhoneCountry(phoneCountry);
        r.setPassword("secret123");
        r.setConfirmPassword("secret123");
        r.setCaptchaKey("k");
        r.setCaptchaCode("c");
        return r;
    }

    @Test
    @DisplayName("日本 080 制式手机号可注册：不限位数，仅要求纯数字")
    void acceptsJapanesePhone() {
        controller.register(req("08012345678", "+81"));
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberService).save(captor.capture());
        assertEquals("08012345678", captor.getValue().getPhone());
        assertEquals("+81", captor.getValue().getPhoneCountry());
    }

    @Test
    @DisplayName("任意位数的纯数字号码均可（不再限制11位）")
    void acceptsAnyDigitLength() {
        controller.register(req("12345", "+1"));
        verify(memberService).save(any(Member.class));
    }

    @Test
    @DisplayName("含非数字字符的手机号被拒绝")
    void rejectsNonDigitPhone() {
        assertThrows(BusinessException.class, () -> controller.register(req("080-1234-5678", "+81")));
        assertThrows(BusinessException.class, () -> controller.register(req("+818012345678", "+81")));
        assertThrows(BusinessException.class, () -> controller.register(req("abc123", "+81")));
    }

    @Test
    @DisplayName("超出存储长度(20)的号码被拒绝")
    void rejectsOverlongPhone() {
        assertThrows(BusinessException.class,
                () -> controller.register(req("123456789012345678901", "+81")));
    }

    @Test
    @DisplayName("非法区号格式净化为 null（区号可选，形如 +81/+1/+886）")
    void sanitizesInvalidCountryCode() {
        controller.register(req("08012345678", "japan"));
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberService).save(captor.capture());
        assertNull(captor.getValue().getPhoneCountry());
    }
}
