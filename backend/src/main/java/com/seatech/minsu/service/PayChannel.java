package com.seatech.minsu.service;

import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;

/** 支付渠道抽象：1 Mock(降级/演示) 2 Stripe 3 LinkTrust */
public interface PayChannel {

    /**
     * 退款模式：
     * INSTANT 同意即退款成功（Mock）；
     * API_ASYNC 调渠道退款 API，回调确认到账（Stripe）；
     * MANUAL_OFFLINE 渠道无退款 API，管理员线下退款后站内确认（LinkTrust）
     */
    enum RefundMode { INSTANT, API_ASYNC, MANUAL_OFFLINE }

    /** 渠道编码：1模拟 2Stripe 3LinkTrust */
    int channel();

    default RefundMode refundMode() {
        return RefundMode.INSTANT;
    }

    /** 该笔支付当前是否具备渠道退款条件（密钥就绪、渠道交易信息完整） */
    default boolean canRefund(Payment payment) {
        return true;
    }

    /**
     * 创建支付。
     * Mock：直接生成成功支付并联动订单；Stripe：创建/复用 PaymentIntent，落待支付 payment 行；
     * LinkTrust：创建/复用待支付 payment 行并返回收银台跳转参数。
     */
    PayCreateResult createPayment(Booking booking, Integer payMethod);

    /**
     * 发起渠道退款。
     *
     * @return 渠道退款单号；Mock 渠道返回 null 表示即时退款成功
     */
    String refund(Payment payment, RefundRecord record);

    /** 查询渠道侧支付状态：0处理中/待支付 1成功 2失败或已取消 */
    int queryStatus(Payment payment);
}
