package com.seatech.minsu.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.testutil.ChainMocks;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款状态机（按渠道 RefundMode 分支）：
 * INSTANT 即时置3；API_ASYNC 渠道退款→1→回调置3；MANUAL_OFFLINE 置1→线下退款→complete 置3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundServiceTest {

    @Mock
    RefundRecordService refundRecordService;
    @Mock
    BookingService bookingService;
    @Mock
    PaymentService paymentService;
    @Mock
    PayChannelRegistry registry;
    @Mock
    PayChannel stripeChannel;
    @Mock
    PayChannel linkTrustChannel;
    @Mock
    PayChannel mockChannel;

    LambdaQueryChainWrapper<Payment> payQuery;
    LambdaUpdateChainWrapper<Payment> payUpdate;

    RefundService refundService;

    Booking booking;
    Payment paidPayment;

    @BeforeEach
    void setUp() {
        payQuery = ChainMocks.queryChain();
        payUpdate = ChainMocks.updateChain();
        when(paymentService.lambdaQuery()).thenReturn(payQuery);
        when(paymentService.lambdaUpdate()).thenReturn(payUpdate);

        when(registry.byCode(1)).thenReturn(mockChannel);
        when(registry.byCode(2)).thenReturn(stripeChannel);
        when(registry.byCode(3)).thenReturn(linkTrustChannel);
        when(registry.refundModeOf(1)).thenReturn(PayChannel.RefundMode.INSTANT);
        when(registry.refundModeOf(2)).thenReturn(PayChannel.RefundMode.API_ASYNC);
        when(registry.refundModeOf(3)).thenReturn(PayChannel.RefundMode.MANUAL_OFFLINE);
        when(mockChannel.refundMode()).thenReturn(PayChannel.RefundMode.INSTANT);
        when(stripeChannel.refundMode()).thenReturn(PayChannel.RefundMode.API_ASYNC);
        when(linkTrustChannel.refundMode()).thenReturn(PayChannel.RefundMode.MANUAL_OFFLINE);

        refundService = new RefundService(refundRecordService, bookingService, paymentService, registry);

        booking = new Booking();
        booking.setId(9L);
        booking.setPayStatus(1);
        booking.setBookingStatus(1);
        when(bookingService.getById(9L)).thenReturn(booking);

        paidPayment = new Payment();
        paidPayment.setId(1L);
        paidPayment.setBookingId(9L);
        paidPayment.setPayStatus(1);
        paidPayment.setStripePaymentIntentId("pi_1");
        when(payQuery.one()).thenReturn(paidPayment);
    }

    private RefundRecord record(int channel, int status) {
        RefundRecord r = new RefundRecord();
        r.setId(7L);
        r.setBookingId(9L);
        r.setRefundAmount(new BigDecimal("100"));
        r.setChannel(channel);
        r.setStatus(status);
        when(refundRecordService.getById(7L)).thenReturn(r);
        return r;
    }

    @Test
    @DisplayName("LinkTrust 单同意：仅置退款中，不调渠道，不动订单")
    void approveManualOfflineOnlyMarksRefunding() {
        RefundRecord r = record(3, 0);
        refundService.approve(7L, 88L);
        assertEquals(1, r.getStatus());
        assertEquals(88L, r.getHandlerId());
        assertNotNull(r.getHandleTime());
        verify(linkTrustChannel, never()).refund(any(), any());
        verify(bookingService, never()).updateById(any(Booking.class));
    }

    @Test
    @DisplayName("Stripe 单同意：调渠道退款并置退款中")
    void approveApiAsyncCallsChannelRefund() {
        RefundRecord r = record(2, 0);
        when(stripeChannel.canRefund(paidPayment)).thenReturn(true);
        when(stripeChannel.refund(paidPayment, r)).thenReturn("re_123");
        refundService.approve(7L, 88L);
        assertEquals(1, r.getStatus());
        assertEquals("re_123", r.getStripeRefundId());
    }

    @Test
    @DisplayName("Stripe 单但渠道不可退（密钥缺失等）：退化为即时退款")
    void approveApiAsyncWithoutCapabilityFallsBackInstant() {
        RefundRecord r = record(2, 0);
        when(stripeChannel.canRefund(paidPayment)).thenReturn(false);
        refundService.approve(7L, 88L);
        assertEquals(3, r.getStatus());
        assertEquals(2, booking.getPayStatus());
        assertEquals(4, booking.getBookingStatus());
        verify(stripeChannel, never()).refund(any(), any());
    }

    @Test
    @DisplayName("Mock 单同意：即时置已退款并联动订单")
    void approveInstantMock() {
        RefundRecord r = record(1, 0);
        refundService.approve(7L, 88L);
        assertEquals(3, r.getStatus());
        assertEquals(2, booking.getPayStatus());
    }

    @Test
    @DisplayName("已处理的申请不可重复同意")
    void approveTwiceRejected() {
        record(3, 1);
        assertThrows(BusinessException.class, () -> refundService.approve(7L, 88L));
    }

    @Test
    @DisplayName("确认线下退款完成：LinkTrust 退款中 → 已退款并联动订单")
    void completeManualOffline() {
        RefundRecord r = record(3, 1);
        refundService.complete(7L, 88L);
        assertEquals(3, r.getStatus());
        assertEquals(88L, r.getHandlerId());
        assertEquals(2, booking.getPayStatus());
        assertEquals(4, booking.getBookingStatus());
    }

    @Test
    @DisplayName("非线下渠道不可手工完成（Stripe 由 webhook 确认）")
    void completeRejectsNonManualChannel() {
        record(2, 1);
        assertThrows(BusinessException.class, () -> refundService.complete(7L, 88L));
    }

    @Test
    @DisplayName("非退款中状态不可完成")
    void completeRejectsWrongStatus() {
        record(3, 0);
        assertThrows(BusinessException.class, () -> refundService.complete(7L, 88L));
    }

    @Test
    @DisplayName("已完成订单退款到账：booking_status 保持3")
    void refundedFinishedBookingKeepsStatus() {
        booking.setBookingStatus(3);
        RefundRecord r = record(1, 0);
        refundService.approve(7L, 88L);
        assertEquals(3, r.getStatus());
        assertEquals(3, booking.getBookingStatus());
    }
}
