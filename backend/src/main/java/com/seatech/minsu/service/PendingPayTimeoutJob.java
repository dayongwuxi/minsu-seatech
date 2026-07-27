package com.seatech.minsu.service;

import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待支付超时释放房态：下单即锁房（booking_status∈{0,1,2} 参与占用判定），
 * 外跳支付（LinkTrust/Stripe）弃单会永久占房，超时自动取消释放。
 * 取消前对存在渠道待支付流水的订单先做一次渠道终确认，防误伤已付款用户。
 * 配置 minsu.booking.pay-timeout-minutes（默认30，0=禁用）。
 */
@Slf4j
@Service
public class PendingPayTimeoutJob {

    private final long timeoutMinutes;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PaymentSyncService paymentSyncService;

    public PendingPayTimeoutJob(@Value("${minsu.booking.pay-timeout-minutes:30}") long timeoutMinutes,
                                BookingService bookingService,
                                PaymentService paymentService,
                                PaymentSyncService paymentSyncService) {
        this.timeoutMinutes = timeoutMinutes;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.paymentSyncService = paymentSyncService;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void run() {
        try {
            cancelExpiredOnce();
        } catch (Exception e) {
            log.error("待支付超时释放任务异常", e);
        }
    }

    /** 扫描并取消一轮超时未支付订单；返回取消数量（核心逻辑，可单测） */
    public int cancelExpiredOnce() {
        if (timeoutMinutes <= 0) {
            return 0;
        }
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Booking> expired = bookingService.lambdaQuery()
                .eq(Booking::getPayStatus, 0)
                .eq(Booking::getBookingStatus, 0)
                .lt(Booking::getCreateTime, deadline)
                .list();
        int cancelled = 0;
        for (Booking booking : expired) {
            Payment pending = paymentService.lambdaQuery()
                    .eq(Payment::getBookingId, booking.getId())
                    .in(Payment::getChannel, 2, 3)
                    .eq(Payment::getPayStatus, 0)
                    .orderByDesc(Payment::getId)
                    .last("LIMIT 1")
                    .one();
            if (pending != null && paymentSyncService.sync(pending) == 1) {
                log.info("超时订单渠道终确认已支付，跳过取消: orderNo={}", booking.getOrderNo());
                continue;
            }
            booking.setBookingStatus(4);
            bookingService.updateById(booking);
            cancelled++;
            log.info("待支付超时自动取消: orderNo={}", booking.getOrderNo());
        }
        return cancelled;
    }
}
