package com.seatech.minsu.service;

import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.LinkTrustTxn;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.service.impl.LinkTrustPayChannel;
import com.seatech.minsu.service.impl.StripePayChannel;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 渠道状态同步（结果页兜底 / 超时 Job / IPN 回调共用）：以渠道侧结果为准落库 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentSyncServiceTest {

    @Mock
    StripePayChannel stripePayChannel;
    @Mock
    LinkTrustPayChannel linkTrustPayChannel;
    @Mock
    PaymentStateService paymentStateService;

    PaymentSyncService syncService;

    @BeforeEach
    void setUp() {
        StripeConfig stripeConfig = new StripeConfig("sk_test", "pk_test", "whsec", "jpy", "card");
        LinkTrustConfig linkTrustConfig = new LinkTrustConfig("M810000022", "u", "q",
                "wechatpay,alipay,unionpay", "jpy");
        syncService = new PaymentSyncService(stripePayChannel, linkTrustPayChannel,
                stripeConfig, linkTrustConfig, paymentStateService);
    }

    private Payment pending(int channel) {
        Payment p = new Payment();
        p.setId(1L);
        p.setPayNo("SR1");
        p.setBookingId(9L);
        p.setChannel(channel);
        p.setPayStatus(0);
        p.setPayMethod(1);
        p.setAmount(new BigDecimal("15000.00"));
        return p;
    }

    private LinkTrustTxn txn(String result, String payMethod, Long amount) {
        LinkTrustTxn t = new LinkTrustTxn();
        t.setTransactionId("10170928104830697177");
        t.setResult(result);
        t.setPayMethod(payMethod);
        t.setAmount(amount);
        return t;
    }

    @Test
    @DisplayName("非待支付的支付行直接返回现状，不触发渠道查询")
    void skipsNonPending() {
        Payment paid = pending(3);
        paid.setPayStatus(1);
        assertEquals(1, syncService.sync(paid));
        verify(paymentStateService, never()).markSucceeded(any(), any());
    }

    @Test
    @DisplayName("Stripe 渠道：查询成功则以 latestCharge 落库")
    void syncsStripeSuccess() {
        Payment p = pending(2);
        when(stripePayChannel.queryStatus(p)).thenReturn(1);
        when(stripePayChannel.latestChargeOf(p)).thenReturn("ch_1");
        assertEquals(1, syncService.sync(p));
        verify(paymentStateService).markSucceeded(p, "ch_1");
    }

    @Test
    @DisplayName("Stripe 渠道：已取消置失败")
    void syncsStripeFailure() {
        Payment p = pending(2);
        when(stripePayChannel.queryStatus(p)).thenReturn(2);
        assertEquals(2, syncService.sync(p));
        verify(paymentStateService).markFailed(p);
    }

    @Test
    @DisplayName("LinkTrust 渠道：反查成功且金额一致 → 校正支付方式并落库")
    void syncsLinkTrustSuccess() throws Exception {
        Payment p = pending(3);
        when(linkTrustPayChannel.query(p)).thenReturn(txn("success", "unionpay", 15000L));
        assertEquals(1, syncService.sync(p));
        assertEquals(7, p.getPayMethod());
        verify(paymentStateService).markSucceeded(p, "10170928104830697177");
    }

    @Test
    @DisplayName("LinkTrust 渠道：金额不符 → 拒绝落成功，留待人工排查")
    void linkTrustAmountMismatchBlocksSuccess() throws Exception {
        Payment p = pending(3);
        when(linkTrustPayChannel.query(p)).thenReturn(txn("success", "alipay", 300L));
        assertEquals(1, syncService.sync(p));
        verify(paymentStateService, never()).markSucceeded(any(), any());
        verify(paymentStateService, never()).markFailed(any());
    }

    @Test
    @DisplayName("LinkTrust 渠道：failure/error 置失败；pending/not found 不动")
    void linkTrustFailureAndPending() throws Exception {
        Payment p = pending(3);
        when(linkTrustPayChannel.query(p)).thenReturn(txn("failure", null, null));
        assertEquals(2, syncService.sync(p));
        verify(paymentStateService).markFailed(p);

        Payment p2 = pending(3);
        when(linkTrustPayChannel.query(p2)).thenReturn(txn("pending", null, null));
        assertEquals(0, syncService.sync(p2));
        verify(paymentStateService, never()).markSucceeded(any(), any());
    }

    @Test
    @DisplayName("LinkTrust 反查异常：保持待支付返回0")
    void linkTrustQueryErrorKeepsPending() throws Exception {
        Payment p = pending(3);
        when(linkTrustPayChannel.query(p)).thenThrow(new IllegalStateException("HTTP 500"));
        assertEquals(0, syncService.sync(p));
        verify(paymentStateService, never()).markSucceeded(any(), any());
        verify(paymentStateService, never()).markFailed(any());
    }

    @Test
    @DisplayName("渠道未启用时不查询（禁用配置）")
    void disabledChannelNoop() throws Exception {
        PaymentSyncService disabled = new PaymentSyncService(stripePayChannel, linkTrustPayChannel,
                new StripeConfig("", "", "", "jpy", "card"),
                new LinkTrustConfig("", "u", "q", "wechatpay", "jpy"), paymentStateService);
        assertEquals(0, disabled.sync(pending(2)));
        assertEquals(0, disabled.sync(pending(3)));
        verify(stripePayChannel, never()).queryStatus(any());
        verify(linkTrustPayChannel, never()).query(any());
    }

    @Test
    @DisplayName("mock 渠道(1)不做外部同步")
    void mockChannelNoop() {
        assertEquals(0, syncService.sync(pending(1)));
        verify(paymentStateService, never()).markSucceeded(any(), anyString());
    }
}
