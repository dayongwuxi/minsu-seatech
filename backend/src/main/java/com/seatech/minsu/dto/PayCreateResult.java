package com.seatech.minsu.dto;

import com.seatech.minsu.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/** PayChannel.createPayment 返回：渠道侧支付上下文 */
@Data
public class PayCreateResult {
    private Payment payment;
    /** Stripe clientSecret（mock 渠道为 null） */
    private String clientSecret;
    private String paymentIntentId;
    /** 托管收银台跳转地址（表单 POST 目标；非跳转型渠道为 null） */
    private String redirectUrl;
    /** 收银台表单参数（LinkTrust: merId/merOrderId/amount） */
    private Map<String, String> redirectParams;
    private BigDecimal amount;
    private String currency;
    /** 服务端 confirm 后需要 3DS 等客户操作 */
    private boolean requiresAction;
    /** 服务端 confirm 后直接成功 */
    private boolean succeeded;
}
