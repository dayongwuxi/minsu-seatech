package com.seatech.minsu.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.testutil.ChainMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 待支付超时释放：过期未支付订单先渠道终确认，再取消释放房态 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PendingPayTimeoutJobTest {

    @Mock
    BookingService bookingService;
    @Mock
    PaymentService paymentService;
    @Mock
    PaymentSyncService paymentSyncService;

    LambdaQueryChainWrapper<Booking> bookingQuery;
    LambdaQueryChainWrapper<Payment> paymentQuery;

    @BeforeEach
    void setUp() {
        bookingQuery = ChainMocks.queryChain();
        paymentQuery = ChainMocks.queryChain();
        when(bookingService.lambdaQuery()).thenReturn(bookingQuery);
        when(paymentService.lambdaQuery()).thenReturn(paymentQuery);
    }

    private PendingPayTimeoutJob job(long minutes) {
        return new PendingPayTimeoutJob(minutes, bookingService, paymentService, paymentSyncService);
    }

    private Booking expiredBooking() {
        Booking b = new Booking();
        b.setId(9L);
        b.setOrderNo("DD1");
        b.setPayStatus(0);
        b.setBookingStatus(0);
        return b;
    }

    @Test
    @DisplayName("配置为0时禁用，不扫描")
    void disabledWhenZero() {
        assertEquals(0, job(0).cancelExpiredOnce());
        verify(bookingService, never()).lambdaQuery();
    }

    @Test
    @DisplayName("超时且无渠道待支付流水：直接取消释放房态")
    void cancelsExpiredWithoutChannelPayment() {
        Booking b = expiredBooking();
        when(bookingQuery.list()).thenReturn(List.of(b));
        when(paymentQuery.one()).thenReturn(null);

        assertEquals(1, job(30).cancelExpiredOnce());
        assertEquals(4, b.getBookingStatus());
        verify(bookingService).updateById(b);
    }

    @Test
    @DisplayName("渠道终确认已支付：不取消（防误伤已付款用户）")
    void skipsWhenChannelReportsPaid() {
        Booking b = expiredBooking();
        Payment pending = new Payment();
        pending.setChannel(3);
        pending.setPayStatus(0);
        when(bookingQuery.list()).thenReturn(List.of(b));
        when(paymentQuery.one()).thenReturn(pending);
        when(paymentSyncService.sync(pending)).thenReturn(1);

        assertEquals(0, job(30).cancelExpiredOnce());
        assertEquals(0, b.getBookingStatus());
        verify(bookingService, never()).updateById(any(Booking.class));
    }

    @Test
    @DisplayName("渠道仍待支付：取消")
    void cancelsWhenChannelStillPending() {
        Booking b = expiredBooking();
        Payment pending = new Payment();
        pending.setChannel(2);
        pending.setPayStatus(0);
        when(bookingQuery.list()).thenReturn(List.of(b));
        when(paymentQuery.one()).thenReturn(pending);
        when(paymentSyncService.sync(pending)).thenReturn(0);

        assertEquals(1, job(30).cancelExpiredOnce());
        assertEquals(4, b.getBookingStatus());
    }

    @Test
    @DisplayName("无过期订单：无操作")
    void noExpiredNoop() {
        when(bookingQuery.list()).thenReturn(List.of());
        assertEquals(0, job(30).cancelExpiredOnce());
        verify(bookingService, never()).updateById(any(Booking.class));
    }
}
