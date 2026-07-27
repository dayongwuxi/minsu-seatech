package com.seatech.minsu.service.impl;

import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.PayChannel;
import com.seatech.minsu.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/** 模拟支付渠道：无 Stripe 密钥时降级使用，支付/退款即时成功 */
@Service
@RequiredArgsConstructor
public class MockPayChannel implements PayChannel {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final StripeConfig stripeConfig;

    @Override
    public int channel() {
        return 1;
    }

    @Override
    public PayCreateResult createPayment(Booking booking, Integer payMethod) {
        Payment payment = new Payment();
        payment.setPayNo(NoGenerator.gen("SR"));
        payment.setBookingId(booking.getId());
        payment.setMemberId(booking.getMemberId());
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(booking.getCurrency() == null ? stripeConfig.getCurrency() : booking.getCurrency());
        payment.setPayMethod(payMethod == null ? 4 : payMethod);
        payment.setChannel(1);
        payment.setPayStatus(1);
        payment.setPayTime(LocalDateTime.now());
        payment.setTransactionNo("MOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        paymentService.save(payment);

        booking.setPayStatus(1);
        bookingService.updateById(booking);

        PayCreateResult result = new PayCreateResult();
        result.setPayment(payment);
        result.setAmount(payment.getAmount());
        result.setCurrency(payment.getCurrency());
        return result;
    }

    @Override
    public String refund(Payment payment, RefundRecord record) {
        // 模拟渠道即时退款成功，无渠道退款单号
        return null;
    }

    @Override
    public int queryStatus(Payment payment) {
        Integer status = payment.getPayStatus();
        if (status == null) {
            return 0;
        }
        return status == 1 ? 1 : status == 2 ? 2 : 0;
    }
}
