package com.seatech.minsu.dto;

import lombok.Data;

/** 发起 LinkTrust 支付请求 */
@Data
public class LinkTrustPayRequest {
    private String orderNo;
    /** 1微信 2支付宝 7银联 */
    private Integer payMethod;
}
