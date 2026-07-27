package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class PayRequest {
    private String orderNo;
    /** 1微信 2支付宝 3银行卡 4其他/余额 */
    private Integer payMethod;
    /** 可选：已保存支付方式(本地 id)，传入时记录 pay_method=3 */
    private Long paymentMethodId;
}
