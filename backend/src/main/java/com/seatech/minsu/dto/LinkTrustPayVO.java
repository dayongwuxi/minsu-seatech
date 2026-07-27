package com.seatech.minsu.dto;

import lombok.Data;

/** LinkTrust 收银台跳转参数：前端据此构建隐藏表单 POST 跳转 */
@Data
public class LinkTrustPayVO {
    private String payUrl;
    private String merId;
    /** = payment.payNo */
    private String merOrderId;
    /** 日元整数（字符串直传表单） */
    private String amount;
    private String payNo;
}
