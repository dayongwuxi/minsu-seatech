package com.seatech.minsu.controller;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.dto.CaptchaVO;
import com.seatech.minsu.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 图形验证码(用户端/管理端共用) */
@RestController
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping({"/api/captcha", "/api/admin/captcha"})
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }
}
