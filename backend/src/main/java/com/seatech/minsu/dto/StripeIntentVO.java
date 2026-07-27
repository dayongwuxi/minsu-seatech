package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StripeIntentVO {
    private String clientSecret;
    private String paymentIntentId;
    private BigDecimal amount;
    private String currency;
    /** 已存卡服务端 confirm 后需要 3DS 等客户操作时为 true, 前端用 clientSecret handleNextAction */
    private boolean requiresAction;
    /** 已存卡服务端 confirm 后直接成功时为 true, 前端直接跳结果页 */
    private boolean succeeded;
}
