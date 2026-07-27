package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.JwtUtil;
import com.seatech.minsu.dto.LoginRequest;
import com.seatech.minsu.dto.LoginVO;
import com.seatech.minsu.dto.MemberVO;
import com.seatech.minsu.dto.RegisterRequest;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.MemberType;
import com.seatech.minsu.service.CaptchaService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.MemberTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/** 用户端注册/登录 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final MemberTypeService memberTypeService;
    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest req) {
        if (!captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPhone())
                || !StringUtils.hasText(req.getPassword())) {
            throw new BusinessException("用户名、手机号、密码不能为空");
        }
        // 手机号国际化：不限位数，仅要求纯数字（支持日本 080 等各国制式）；20 为列宽上限
        if (!req.getPhone().matches("\\d{1,20}")) {
            throw new BusinessException("手机号必须为数字");
        }
        if (!Objects.equals(req.getPassword(), req.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (memberService.lambdaQuery().eq(Member::getUsername, req.getUsername()).count() > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (memberService.lambdaQuery().eq(Member::getPhone, req.getPhone()).count() > 0) {
            throw new BusinessException("手机号已被注册");
        }
        Member member = new Member();
        member.setMemberNo(NoGenerator.gen("M"));
        member.setUsername(req.getUsername());
        member.setPhone(req.getPhone());
        // 区号可选，仅接受 +数字 形式，其余净化为 null
        String country = req.getPhoneCountry();
        member.setPhoneCountry(country != null && country.matches("\\+\\d{1,4}") ? country : null);
        member.setPassword(passwordEncoder.encode(req.getPassword()));
        member.setMemberTypeId(1L);
        member.setStatus(1);
        memberService.save(member);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest req) {
        if (!captchaService.verify(req.getCaptchaKey(), req.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        Member member = memberService.lambdaQuery()
                .eq(Member::getUsername, req.getAccount())
                .or()
                .eq(Member::getPhone, req.getAccount())
                .list()
                .stream().findFirst().orElse(null);
        if (member == null || !passwordEncoder.matches(req.getPassword(), member.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系客服");
        }
        String typeName = null;
        if (member.getMemberTypeId() != null) {
            MemberType type = memberTypeService.getById(member.getMemberTypeId());
            typeName = type == null ? null : type.getTypeName();
        }
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.createToken(member.getId(), JwtUtil.AUD_MEMBER));
        vo.setMember(MemberVO.of(member, typeName, true));
        return Result.ok(vo);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
