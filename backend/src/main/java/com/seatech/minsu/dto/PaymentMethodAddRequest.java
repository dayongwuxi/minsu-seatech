package com.seatech.minsu.dto;

import lombok.Data;

/**
 * 绑卡请求。
 * Stripe 模式仅需 stripePaymentMethodId；Mock 模式提交 brand/last4/有效期。
 * cardNumber 为防御字段：出现超过4位数字的完整卡号直接拒绝（敏感数据零落库）。
 */
@Data
public class PaymentMethodAddRequest {
    private String stripePaymentMethodId;
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private String holderName;
    /** 禁止提交完整卡号，此字段仅用于拦截校验 */
    private String cardNumber;
}
