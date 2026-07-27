package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 会员已保存支付方式（卡号/CVC 等敏感信息只存于 Stripe，本表仅缓存展示元数据） */
@Data
@TableName("member_payment_method")
public class MemberPaymentMethod {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    /** 1模拟 2Stripe */
    private Integer channel;
    /** Stripe PaymentMethod id (channel=2) */
    private String stripePaymentMethodId;
    /** 1银行卡 2其他 */
    private Integer methodType;
    /** 卡品牌 visa/mastercard/jcb/unionpay */
    private String brand;
    /** 卡号后四位 */
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    /** 持卡人(可选) */
    private String holderName;
    /** 默认支付方式 */
    private Integer isDefault;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
