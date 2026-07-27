package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 退款记录 */
@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookingId;
    private Long memberId;
    private BigDecimal refundAmount;
    private String reason;
    /** 0申请中 1已同意(退款中) 2已拒绝 3已退款 */
    private Integer status;
    /** 1模拟 2Stripe 3LinkTrust */
    private Integer channel;
    /** 渠道退款单号（Stripe Refund id；LinkTrust 线下退款无） */
    private String stripeRefundId;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    /** 处理管理员id */
    private Long handlerId;
}
