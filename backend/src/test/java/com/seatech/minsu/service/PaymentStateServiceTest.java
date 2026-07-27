package com.seatech.minsu.service;

import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 支付状态落库：成功/失败/幂等/订单已取消场景 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentStateServiceTest {

    @Mock
    PaymentService paymentService;
    @Mock
    BookingService bookingService;

    PaymentStateService stateService;

    Payment payment;
    Booking booking;

    @BeforeEach
    void setUp() {
        stateService = new PaymentStateService(paymentService, bookingService);
        payment = new Payment();
        payment.setId(1L);
        payment.setPayNo("SR1");
        payment.setBookingId(9L);
        payment.setPayStatus(0);
        payment.setAmount(new BigDecimal("100"));
        booking = new Booking();
        booking.setId(9L);
        booking.setPayStatus(0);
        booking.setBookingStatus(0);
        when(bookingService.getById(9L)).thenReturn(booking);
    }

    @Test
    @DisplayName("支付成功：payment 置1、记交易号，booking 联动置已支付")
    void markSucceededUpdatesPaymentAndBooking() {
        stateService.markSucceeded(payment, "10170928104830697177");
        assertEquals(1, payment.getPayStatus());
        assertEquals("10170928104830697177", payment.getTransactionNo());
        assertNotNull(payment.getPayTime());
        assertEquals(1, booking.getPayStatus());
        verify(paymentService).updateById(payment);
        verify(bookingService).updateById(booking);
    }

    @Test
    @DisplayName("重复回调幂等：已成功的支付不再落库")
    void markSucceededIdempotent() {
        payment.setPayStatus(1);
        stateService.markSucceeded(payment, "tx2");
        verify(paymentService, never()).updateById(any(Payment.class));
        verify(bookingService, never()).updateById(any(Booking.class));
    }

    @Test
    @DisplayName("订单已被取消后支付成功：仍记收款（待人工退款），订单保持已取消")
    void markSucceededAfterBookingCancelled() {
        booking.setBookingStatus(4);
        stateService.markSucceeded(payment, "tx3");
        assertEquals(1, payment.getPayStatus());
        assertEquals(1, booking.getPayStatus());
        assertEquals(4, booking.getBookingStatus());
    }

    @Test
    @DisplayName("支付失败：仅待支付可置失败，订单保持可重试")
    void markFailedOnlyFromPending() {
        stateService.markFailed(payment);
        assertEquals(2, payment.getPayStatus());
        verify(bookingService, never()).updateById(any(Booking.class));

        payment.setPayStatus(1);
        stateService.markFailed(payment);
        assertEquals(1, payment.getPayStatus());
    }
}
