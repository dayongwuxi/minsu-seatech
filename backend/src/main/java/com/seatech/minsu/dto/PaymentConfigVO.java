package com.seatech.minsu.dto;

import lombok.Data;

import java.util.List;

/** 支付配置(前端据此渲染支付方式；多渠道并存，新渠道在此追加启用状态) */
@Data
public class PaymentConfigVO {
    private boolean stripeEnabled;
    private String publishableKey;
    private List<String> methods;
    private String currency;
    /** LinkTrust 跳转支付渠道 */
    private boolean linktrustEnabled;
    /** LinkTrust 支持的 pay_method 编码：1微信 2支付宝 7银联 */
    private List<Integer> linktrustMethods;
}
