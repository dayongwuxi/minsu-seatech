package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class StripeIntentRequest {
    private String orderNo;
    /** 可选：已保存支付方式(本地 member_payment_method.id)，走 customer+payment_method 直接确认 */
    private Long paymentMethodId;
}
