package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 价格明细报价（Airbnb 式 breakdown） */
@Data
public class QuoteVO {
    private Integer nights;
    private BigDecimal unitPrice;
    /** 房费小计=单价×晚数 */
    private BigDecimal roomFee;
    /** 促销优惠额(取优惠最大一条) */
    private BigDecimal promoDiscount;
    /** 命中促销名称 */
    private String promoName;
    /** 命中促销id */
    private Long promotionId;
    /** 实际使用的优惠码(自动促销为 null) */
    private String promoCode;
    /** 会员折扣额(基于扣除促销后余额) */
    private BigDecimal memberDiscount;
    /** 订单总额(下限0.01) */
    private BigDecimal totalAmount;
    private String currency;
}
