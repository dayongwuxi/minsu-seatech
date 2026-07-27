package com.seatech.minsu.controller;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.PaymentMethodAddRequest;
import com.seatech.minsu.dto.SetupIntentVO;
import com.seatech.minsu.entity.MemberPaymentMethod;
import com.seatech.minsu.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 会员支付方式管理（绑卡） */
@RestController
@RequestMapping("/api/user/payment-methods")
@RequiredArgsConstructor
public class UserPaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public Result<List<MemberPaymentMethod>> list() {
        return Result.ok(paymentMethodService.listByMember(AuthContext.get()));
    }

    /** 绑卡 SetupIntent：Stripe 启用时返回 clientSecret；未启用返回 {clientSecret:null, mock:true} */
    @PostMapping("/setup-intent")
    public Result<SetupIntentVO> setupIntent() {
        return Result.ok(paymentMethodService.createSetupIntent(AuthContext.get()));
    }

    @PostMapping
    public Result<MemberPaymentMethod> add(@RequestBody PaymentMethodAddRequest req) {
        return Result.ok(paymentMethodService.add(AuthContext.get(), req));
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        paymentMethodService.setDefault(AuthContext.get(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(AuthContext.get(), id);
        return Result.ok();
    }
}
