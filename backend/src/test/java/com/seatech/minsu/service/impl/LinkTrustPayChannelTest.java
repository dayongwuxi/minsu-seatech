package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.dto.LinkTrustTxn;
import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.service.PayChannel;
import com.seatech.minsu.service.PaymentService;
import com.seatech.minsu.testutil.ChainMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.dao.DuplicateKeyException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** LinkTrust 支付渠道单测：创建/复用支付行、跳转参数、状态查询、退款禁用 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinkTrustPayChannelTest {

    @Mock
    PaymentService paymentService;
    @Mock
    LinkTrustClient client;

    LambdaQueryChainWrapper<Payment> chain;
    LinkTrustPayChannel channel;

    @BeforeEach
    void setUp() {
        chain = ChainMocks.queryChain();
        when(paymentService.lambdaQuery()).thenReturn(chain);
        LinkTrustConfig config = new LinkTrustConfig("M810000022",
                "https://pay.linktrust-pay.com/webpay/payform",
                "https://pay.linktrust-pay.com/webpay/transactions",
                "wechatpay,alipay,unionpay", "jpy");
        channel = new LinkTrustPayChannel(config, client, paymentService);
    }

    private Booking booking() {
        Booking b = new Booking();
        b.setId(9L);
        b.setMemberId(5L);
        b.setOrderNo("DD202607240001");
        b.setTotalAmount(new BigDecimal("15000.00"));
        b.setCurrency("jpy");
        return b;
    }

    @Test
    @DisplayName("渠道编码=3，退款模式=线下人工")
    void channelMeta() {
        assertEquals(3, channel.channel());
        assertEquals(PayChannel.RefundMode.MANUAL_OFFLINE, channel.refundMode());
    }

    @Test
    @DisplayName("未配置 merId 时拒绝创建支付")
    void createRejectsWhenDisabled() {
        LinkTrustPayChannel disabled = new LinkTrustPayChannel(
                new LinkTrustConfig("", "u", "q", "wechatpay", "jpy"), client, paymentService);
        assertThrows(BusinessException.class, () -> disabled.createPayment(booking(), 1));
    }

    @Test
    @DisplayName("新建支付行：channel=3 待支付，merOrderId=payNo，金额整数直传")
    void createsNewPendingPayment() {
        when(chain.one()).thenReturn(null);
        PayCreateResult result = channel.createPayment(booking(), 1);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentService).save(captor.capture());
        Payment saved = captor.getValue();
        assertEquals(3, saved.getChannel());
        assertEquals(0, saved.getPayStatus());
        assertEquals(1, saved.getPayMethod());
        assertEquals(9L, saved.getBookingId());
        assertEquals(5L, saved.getMemberId());
        assertTrue(saved.getPayNo().startsWith("SR"));
        assertEquals(0, saved.getAmount().compareTo(new BigDecimal("15000.00")));
        assertEquals("jpy", saved.getCurrency());

        assertEquals("https://pay.linktrust-pay.com/webpay/payform", result.getRedirectUrl());
        assertEquals("M810000022", result.getRedirectParams().get("merId"));
        assertEquals(saved.getPayNo(), result.getRedirectParams().get("merOrderId"));
        assertEquals("15000", result.getRedirectParams().get("amount"));
    }

    @Test
    @DisplayName("复用未完成支付行：不新建，支付方式变化时更新")
    void reusesPendingPayment() {
        Payment pending = new Payment();
        pending.setId(1L);
        pending.setPayNo("SR202607240001");
        pending.setBookingId(9L);
        pending.setPayMethod(1);
        pending.setPayStatus(0);
        pending.setChannel(3);
        pending.setAmount(new BigDecimal("15000.00"));
        when(chain.one()).thenReturn(pending);

        PayCreateResult result = channel.createPayment(booking(), 2);
        verify(paymentService, never()).save(any());
        verify(paymentService).updateById(pending);
        assertEquals(2, pending.getPayMethod());
        assertSame(pending, result.getPayment());
        assertEquals("SR202607240001", result.getRedirectParams().get("merOrderId"));
    }

    @Test
    @DisplayName("复用时支付方式一致则不更新")
    void reuseWithoutMethodChangeSkipsUpdate() {
        Payment pending = new Payment();
        pending.setPayNo("SR1");
        pending.setPayMethod(1);
        pending.setAmount(new BigDecimal("15000"));
        when(chain.one()).thenReturn(pending);

        channel.createPayment(booking(), 1);
        verify(paymentService, never()).updateById(any());
    }

    @Test
    @DisplayName("并发闸门：插入撞唯一约束(uk_payment_pending)时回落复用对方已建的待支付行")
    void duplicateInsertFallsBackToReuse() {
        Payment other = new Payment();
        other.setId(2L);
        other.setPayNo("SR_OTHER");
        other.setPayMethod(1);
        other.setAmount(new BigDecimal("15000"));
        // 首查为空 → 尝试插入撞约束 → 重查拿到并发请求已建的行
        when(chain.one()).thenReturn(null, other);
        doThrow(new DuplicateKeyException("uk_payment_pending")).when(paymentService).save(any());

        PayCreateResult result = channel.createPayment(booking(), 2);
        assertSame(other, result.getPayment());
        assertEquals("SR_OTHER", result.getRedirectParams().get("merOrderId"));
        // 支付方式以本次请求为准更新
        assertEquals(2, other.getPayMethod());
        verify(paymentService).updateById(other);
    }

    @Test
    @DisplayName("并发闸门：撞约束后重查仍无待支付行(对方已支付) → 明确报错让用户刷新")
    void duplicateInsertWithoutPendingRejects() {
        when(chain.one()).thenReturn(null, (Payment) null);
        doThrow(new DuplicateKeyException("uk_payment_pending")).when(paymentService).save(any());
        assertThrows(BusinessException.class, () -> channel.createPayment(booking(), 1));
    }

    @Test
    @DisplayName("LinkTrust 不支持 API 退款")
    void refundUnsupported() {
        assertThrows(BusinessException.class, () -> channel.refund(new Payment(), null));
    }

    @Test
    @DisplayName("状态查询：反查 transactions 并映射；异常按处理中返回")
    void queryStatusMapsResult() throws Exception {
        Payment payment = new Payment();
        payment.setPayNo("SR1");
        LinkTrustTxn txn = new LinkTrustTxn();
        txn.setResult("success");
        when(client.queryTransaction("SR1")).thenReturn(txn);
        assertEquals(1, channel.queryStatus(payment));

        txn.setResult("failure");
        assertEquals(2, channel.queryStatus(payment));

        when(client.queryTransaction("SR1")).thenThrow(new IllegalStateException("HTTP 500"));
        assertEquals(0, channel.queryStatus(payment));
    }
}
