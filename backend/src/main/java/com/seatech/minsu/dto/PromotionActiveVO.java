package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Promotion;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 用户端促销标签（不泄露未公开优惠码） */
@Data
public class PromotionActiveVO {

    private Long id;
    private String name;
    private Integer type;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private BigDecimal thresholdAmount;
    private Integer minNights;
    private Integer advanceDays;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 是否需输入优惠码(码本身不返回) */
    private Boolean hasCoupon;

    public static PromotionActiveVO of(Promotion p) {
        PromotionActiveVO vo = new PromotionActiveVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setType(p.getType());
        vo.setDiscountRate(p.getDiscountRate());
        vo.setDiscountAmount(p.getDiscountAmount());
        vo.setThresholdAmount(p.getThresholdAmount());
        vo.setMinNights(p.getMinNights());
        vo.setAdvanceDays(p.getAdvanceDays());
        vo.setStartDate(p.getStartDate());
        vo.setEndDate(p.getEndDate());
        vo.setHasCoupon(p.getCouponCode() != null && !p.getCouponCode().isBlank());
        return vo;
    }
}
