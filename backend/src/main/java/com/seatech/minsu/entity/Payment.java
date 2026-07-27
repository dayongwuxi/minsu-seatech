package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付记录(收入管理数据源) */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 支付流水号 SR+yyyyMMdd+4位随机 */
    private String payNo;
    private Long bookingId;
    private Long memberId;
    private BigDecimal amount;
    private String currency;
    /** 1微信 2支付宝 3银行卡 4其他/聚合 7银联 */
    private Integer payMethod;
    /** 1模拟 2Stripe 3LinkTrust */
    private Integer channel;
    /** 0待支付 1成功 2失败 3已退款 */
    private Integer payStatus;
    private LocalDateTime payTime;
    /** 第三方交易单号 */
    private String transactionNo;
    /** Stripe PaymentIntent id */
    private String stripePaymentIntentId;
    private LocalDateTime createTime;
}
