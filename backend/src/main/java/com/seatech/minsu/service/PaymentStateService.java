package com.seatech.minsu.service;

import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 支付状态落库：webhook / 结果页主动查询 / mock 支付共用 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStateService {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    /** 支付成功：payment 置成功并联动 booking.pay_status=1 */
    public void markSucceeded(Payment payment, String transactionNo) {
        if (payment.getPayStatus() != null && payment.getPayStatus() == 1) {
            return;
        }
        payment.setPayStatus(1);
        if (payment.getPayTime() == null) {
            payment.setPayTime(LocalDateTime.now());
        }
        if (transactionNo != null && !transactionNo.isBlank()) {
            payment.setTransactionNo(transactionNo);
        }
        paymentService.updateById(payment);

        Booking booking = bookingService.getById(payment.getBookingId());
        if (booking != null && booking.getPayStatus() != null && booking.getPayStatus() == 0) {
            booking.setPayStatus(1);
            bookingService.updateById(booking);
            // 超时释放后渠道又确认收款的竞态：钱已收、房已释放，需人工退款处理
            if (booking.getBookingStatus() != null && booking.getBookingStatus() == 4) {
                log.warn("订单已取消但收到支付成功，请人工发起退款: orderNo={}, payNo={}",
                        booking.getOrderNo(), payment.getPayNo());
            }
        }
        log.info("支付成功: payNo={}, bookingId={}", payment.getPayNo(), payment.getBookingId());
    }

    /** 支付失败：仅记录 payment 状态，订单保持待支付可重试 */
    public void markFailed(Payment payment) {
        if (payment.getPayStatus() == null || payment.getPayStatus() == 0) {
            payment.setPayStatus(2);
            paymentService.updateById(payment);
            log.info("支付失败: payNo={}, bookingId={}", payment.getPayNo(), payment.getBookingId());
        }
    }
}
