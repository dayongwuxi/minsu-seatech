package com.seatech.minsu.dto;

import lombok.Data;

/** LinkTrust transactions 反查/IPN 结果（result: not found/success/failure/pending/error） */
@Data
public class LinkTrustTxn {
    /** LTP 系统交易ID */
    private String transactionId;
    private String result;
    /** wechatpay / alipay / unionpay */
    private String payMethod;
    /** 决済金额（日元整数）；缺失或非法为 null */
    private Long amount;
}
