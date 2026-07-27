package com.seatech.minsu.service.impl;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.dto.LinkTrustTxn;
import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.service.PayChannel;
import com.seatech.minsu.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LinkTrust Pay 渠道（HTML リンク跳转收银台，支持 wechatpay/alipay/unionpay）。
 * merOrderId = payment.payNo（每次支付尝试唯一，IPN/反查按此定位）。
 * 无退款 API：RefundMode.MANUAL_OFFLINE，管理员在渠道后台手工退款后站内确认。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkTrustPayChannel implements PayChannel {

    private final LinkTrustConfig config;
    private final LinkTrustClient client;
    private final PaymentService paymentService;

    @Override
    public int channel() {
        return 3;
    }

    @Override
    public RefundMode refundMode() {
        return RefundMode.MANUAL_OFFLINE;
    }

    @Override
    public boolean canRefund(Payment payment) {
        return false;
    }

    @Override
    public PayCreateResult createPayment(Booking booking, Integer payMethod) {
        ensureEnabled();
        // 复用未完成的支付行（幂等：重复点击/刷新不再新建，merOrderId 保持稳定）
        Payment payment = findPending(booking.getId());
        if (payment == null) {
            Payment fresh = new Payment();
            fresh.setPayNo(NoGenerator.gen("SR"));
            fresh.setBookingId(booking.getId());
            fresh.setMemberId(booking.getMemberId());
            fresh.setAmount(booking.getTotalAmount());
            fresh.setCurrency(booking.getCurrency());
            fresh.setPayMethod(payMethod == null ? 4 : payMethod);
            fresh.setChannel(3);
            fresh.setPayStatus(0);
            try {
                paymentService.save(fresh);
                payment = fresh;
            } catch (DuplicateKeyException e) {
                // 并发闸门 uk_payment_pending 命中：另一请求已建待支付行，回落复用
                log.info("并发创建 LinkTrust 支付流水命中唯一约束，回落复用: bookingId={}", booking.getId());
                payment = findPending(booking.getId());
                if (payment == null) {
                    throw new BusinessException("订单支付状态已变化，请刷新后重试");
                }
            }
        }
        if (payMethod != null && !Objects.equals(payment.getPayMethod(), payMethod)) {
            payment.setPayMethod(payMethod);
            paymentService.updateById(payment);
        }

        PayCreateResult result = new PayCreateResult();
        result.setPayment(payment);
        result.setAmount(payment.getAmount());
        result.setCurrency(payment.getCurrency());
        result.setRedirectUrl(config.getPayformUrl());
        Map<String, String> params = new LinkedHashMap<>();
        params.put("merId", config.getMerId());
        params.put("merOrderId", payment.getPayNo());
        params.put("amount", String.valueOf(config.toChannelAmount(payment.getAmount())));
        result.setRedirectParams(params);
        return result;
    }

    @Override
    public String refund(Payment payment, RefundRecord record) {
        throw new BusinessException("LinkTrust 渠道不支持 API 退款，请在渠道商户后台手工退款后确认");
    }

    @Override
    public int queryStatus(Payment payment) {
        try {
            return LinkTrustClient.mapResultStatus(query(payment).getResult());
        } catch (Exception e) {
            log.warn("LinkTrust 查询支付状态失败: payNo={}", payment.getPayNo(), e);
            return 0;
        }
    }

    /** 反查渠道交易（结果同步/IPN 验证共用）；异常上抛由调用方决定重试语义 */
    public LinkTrustTxn query(Payment payment) throws Exception {
        ensureEnabled();
        return client.queryTransaction(payment.getPayNo());
    }

    private Payment findPending(Long bookingId) {
        return paymentService.lambdaQuery()
                .eq(Payment::getBookingId, bookingId)
                .eq(Payment::getChannel, 3)
                .eq(Payment::getPayStatus, 0)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
    }

    private void ensureEnabled() {
        if (!config.isEnabled()) {
            throw new BusinessException("LinkTrust 支付未启用");
        }
    }
}
