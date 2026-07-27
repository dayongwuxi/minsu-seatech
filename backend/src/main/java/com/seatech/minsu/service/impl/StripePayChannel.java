package com.seatech.minsu.service.impl;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.MemberPaymentMethod;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.service.PayChannel;
import com.seatech.minsu.service.PaymentService;
import com.seatech.minsu.service.PaymentStateService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/** Stripe 支付渠道（PaymentIntents，支持已存卡 customer+payment_method 直接确认） */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripePayChannel implements PayChannel {

    private final StripeConfig stripeConfig;
    private final PaymentService paymentService;
    private final PaymentStateService paymentStateService;

    @Override
    public int channel() {
        return 2;
    }

    @Override
    public RefundMode refundMode() {
        return RefundMode.API_ASYNC;
    }

    @Override
    public boolean canRefund(Payment payment) {
        return stripeConfig.isEnabled()
                && payment != null && StringUtils.hasText(payment.getStripePaymentIntentId());
    }

    @Override
    public PayCreateResult createPayment(Booking booking, Integer payMethod) {
        return createPayment(booking, null, null);
    }

    /**
     * 创建/复用 PaymentIntent 并落待支付 payment 行。
     *
     * @param customerId  已存卡付款时的 Stripe Customer id（否则 null）
     * @param savedMethod 已保存支付方式（channel=2）；附加 customer+payment_method，
     *                    不设置 setup_future_usage，off_session 保持默认 false，由前端 confirm
     */
    public PayCreateResult createPayment(Booking booking, String customerId, MemberPaymentMethod savedMethod) {
        ensureEnabled();
        boolean useSaved = savedMethod != null && StringUtils.hasText(savedMethod.getStripePaymentMethodId());
        long minorAmount = stripeConfig.toMinorUnits(booking.getTotalAmount());
        try {
            // 复用未完成的 intent，避免重复创建
            Payment pending = paymentService.lambdaQuery()
                    .eq(Payment::getBookingId, booking.getId())
                    .eq(Payment::getChannel, 2)
                    .eq(Payment::getPayStatus, 0)
                    .isNotNull(Payment::getStripePaymentIntentId)
                    .orderByDesc(Payment::getId)
                    .last("LIMIT 1")
                    .one();
            if (pending != null) {
                PaymentIntent intent = PaymentIntent.retrieve(pending.getStripePaymentIntentId());
                if ("succeeded".equals(intent.getStatus())) {
                    paymentStateService.markSucceeded(pending, intent.getLatestCharge());
                    throw new BusinessException("订单已支付，请勿重复支付");
                }
                boolean amountMatch = intent.getAmount() != null && intent.getAmount().longValue() == minorAmount;
                boolean savedMatch = !useSaved
                        || (Objects.equals(intent.getCustomer(), customerId)
                            && Objects.equals(intent.getPaymentMethod(), savedMethod.getStripePaymentMethodId()));
                if (!"canceled".equals(intent.getStatus()) && amountMatch && savedMatch) {
                    if (useSaved && "requires_confirmation".equals(intent.getStatus())) {
                        intent = intent.confirm();
                    }
                    return afterConfirm(pending, intent, useSaved);
                }
            }

            PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                    .setAmount(minorAmount)
                    .setCurrency(stripeConfig.getCurrency())
                    .putMetadata("orderNo", booking.getOrderNo())
                    .putMetadata("bookingId", String.valueOf(booking.getId()));
            if (useSaved) {
                // 已存卡：附加 customer+payment_method 并服务端 confirm(on_session)；
                // 3DS 等场景返回 requires_action，由前端 handleNextAction 完成
                builder.setCustomer(customerId)
                        .setPaymentMethod(savedMethod.getStripePaymentMethodId())
                        .addPaymentMethodType("card")
                        .setConfirm(true);
            } else {
                for (String method : stripeConfig.getMethodList()) {
                    builder.addPaymentMethodType(method);
                }
            }
            PaymentIntent intent = PaymentIntent.create(builder.build());

            Payment payment = new Payment();
            payment.setPayNo(NoGenerator.gen("SR"));
            payment.setBookingId(booking.getId());
            payment.setMemberId(booking.getMemberId());
            payment.setAmount(booking.getTotalAmount());
            payment.setCurrency(stripeConfig.getCurrency());
            payment.setPayMethod(useSaved ? 3 : 4);
            payment.setChannel(2);
            payment.setPayStatus(0);
            payment.setStripePaymentIntentId(intent.getId());
            paymentService.save(payment);
            return afterConfirm(payment, intent, useSaved);
        } catch (DuplicateKeyException e) {
            // 并发闸门 uk_payment_pending 命中：另一请求已建待支付行，重走复用分支
            // （本次多创建的 PaymentIntent 未被确认，会在 Stripe 侧自动过期）
            log.info("并发创建 Stripe 支付流水命中唯一约束，重试复用: bookingId={}", booking.getId());
            return createPayment(booking, customerId, savedMethod);
        } catch (StripeException e) {
            log.error("Stripe 创建支付失败: orderNo={}", booking.getOrderNo(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "支付渠道异常，请稍后重试");
        }
    }

    @Override
    public String refund(Payment payment, RefundRecord record) {
        ensureEnabled();
        if (payment.getStripePaymentIntentId() == null) {
            throw new BusinessException("该支付无 Stripe 交易信息，无法渠道退款");
        }
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .putMetadata("refundRecordId", String.valueOf(record.getId()))
                    .putMetadata("bookingId", String.valueOf(record.getBookingId()))
                    .build();
            Refund refund = Refund.create(params);
            return refund.getId();
        } catch (StripeException e) {
            log.error("Stripe 退款失败: paymentId={}", payment.getId(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR, "退款渠道异常: " + e.getMessage());
        }
    }

    @Override
    public int queryStatus(Payment payment) {
        ensureEnabled();
        if (payment.getStripePaymentIntentId() == null) {
            return 0;
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            if ("succeeded".equals(intent.getStatus())) {
                return 1;
            }
            if ("canceled".equals(intent.getStatus())) {
                return 2;
            }
            return 0;
        } catch (StripeException e) {
            log.warn("Stripe 查询支付状态失败: intent={}", payment.getStripePaymentIntentId(), e);
            return 0;
        }
    }

    /** 结果页兜底同步用：取回 intent 的 latest charge 作为交易单号 */
    public String latestChargeOf(Payment payment) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            return intent.getLatestCharge();
        } catch (StripeException e) {
            return null;
        }
    }

    private void ensureEnabled() {
        if (!stripeConfig.isEnabled()) {
            throw new BusinessException("Stripe 支付未启用");
        }
    }

    private PayCreateResult buildResult(Payment payment, PaymentIntent intent) {
        PayCreateResult result = new PayCreateResult();
        result.setPayment(payment);
        result.setClientSecret(intent.getClientSecret());
        result.setPaymentIntentId(intent.getId());
        result.setAmount(payment.getAmount());
        result.setCurrency(payment.getCurrency());
        return result;
    }

    /** 已存卡服务端 confirm 后按状态收敛: 成功直接落库; 需 3DS 交给前端 handleNextAction; 被拒即报错 */
    private PayCreateResult afterConfirm(Payment payment, PaymentIntent intent, boolean useSaved) {
        PayCreateResult result = buildResult(payment, intent);
        if (!useSaved) {
            return result;
        }
        switch (intent.getStatus()) {
            case "succeeded" -> {
                paymentStateService.markSucceeded(payment, intent.getLatestCharge());
                result.setSucceeded(true);
            }
            case "requires_action" -> result.setRequiresAction(true);
            case "requires_payment_method" ->
                    throw new BusinessException("该银行卡支付被拒绝，请更换支付方式");
            default -> { /* processing 等中间态交给结果页轮询 */ }
        }
        return result;
    }
}
