package com.seatech.minsu.controller;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.dto.PromotionActiveVO;
import com.seatech.minsu.entity.Promotion;
import com.seatech.minsu.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** 用户端促销标签（免登录） */
@RestController
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** 适用中的促销；coupon_code 非空的只返回名称与门槛，不返回码本身 */
    @GetMapping("/api/promotions/active")
    public Result<List<PromotionActiveVO>> active(@RequestParam(required = false) Long roomId) {
        LocalDate today = LocalDate.now();
        var chain = promotionService.lambdaQuery()
                .eq(Promotion::getStatus, 1)
                .le(Promotion::getStartDate, today)
                .ge(Promotion::getEndDate, today);
        if (roomId != null) {
            chain.and(w -> w.isNull(Promotion::getRoomId).or().eq(Promotion::getRoomId, roomId));
        } else {
            chain.isNull(Promotion::getRoomId);
        }
        List<Promotion> list = chain.orderByAsc(Promotion::getId).list();
        List<PromotionActiveVO> vos = list.stream()
                .filter(p -> p.getUsageLimit() == null
                        || p.getUsedCount() == null
                        || p.getUsedCount() < p.getUsageLimit())
                .map(PromotionActiveVO::of)
                .collect(Collectors.toList());
        return Result.ok(vos);
    }
}
