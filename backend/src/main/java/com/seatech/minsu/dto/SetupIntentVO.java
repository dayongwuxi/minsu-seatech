package com.seatech.minsu.dto;

import lombok.Data;

/** 绑卡 SetupIntent 返回：Stripe 未启用时 clientSecret=null, mock=true */
@Data
public class SetupIntentVO {
    private String clientSecret;
    private boolean mock;
}
