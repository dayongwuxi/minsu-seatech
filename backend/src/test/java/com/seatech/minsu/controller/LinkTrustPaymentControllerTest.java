package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.dto.LinkTrustPayRequest;
import com.seatech.minsu.dto.LinkTrustPayVO;
import com.seatech.minsu.dto.LinkTrustTxn;
import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.PaymentService;
import com.seatech.minsu.service.PaymentSyncService;
import com.seatech.minsu.service.impl.LinkTrustPayChannel;
import com.seatech.minsu.testutil.ChainMocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LinkTrust 支付端点：发起支付返回收银台表单参数；
 * IPN 回调无签名 → 一律服务端反查 transactions 为准（防伪造通知）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinkTrustPaymentControllerTest {

    @Mock
    BookingService bookingService;
    @Mock
    PaymentService paymentService;
    @Mock
    LinkTrustPayChannel linkTrustPayChannel;
    @Mock
    PaymentSyncService paymentSyncService;

    LambdaQueryChainWrapper<Booking> bookingQuery;
    LambdaQueryChainWrapper<Payment> paymentQuery;

    LinkTrustPaymentController controller;

    Booking booking;

    @BeforeEach
    void setUp() {
        bookingQuery = ChainMocks.queryChain();
        paymentQuery = ChainMocks.queryChain();
        when(bookingService.lambdaQuery()).thenReturn(bookingQuery);
        when(paymentService.lambdaQuery()).thenReturn(paymentQuery);

        LinkTrustConfig config = new LinkTrustConfig("M810000022",
                "https://pay.linktrust-pay.com/webpay/payform",
                "https://pay.linktrust-pay.com/webpay/transactions",
                "wechatpay,alipay,unionpay", "jpy");
        controller = new LinkTrustPaymentController(bookingService, paymentService,
                config, linkTrustPayChannel, paymentSyncService);

        booking = new Booking();
        booking.setId(9L);
        booking.setMemberId(5L);
        booking.setOrderNo("DD1");
        booking.setPayStatus(0);
        booking.setBookingStatus(0);
        booking.setTotalAmount(new BigDecimal("15000"));
        when(bookingQuery.one()).thenReturn(booking);
        AuthContext.set(5L);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    private LinkTrustPayRequest req(Integer payMethod) {
        LinkTrustPayRequest r = new LinkTrustPayRequest();
        r.setOrderNo("DD1");
        r.setPayMethod(payMethod);
        return r;
    }

    private Map<String, String> ipn(String merId, String merOrderId, String result) {
        Map<String, String> p = new HashMap<>();
        p.put("transactionId", "10170928104830697177");
        p.put("merId", merId);
        p.put("merOrderId", merOrderId);
        p.put("result", result);
        p.put("payMethod", "alipay");
        p.put("amount", "15000");
        return p;
    }

    @Test
    @DisplayName("发起支付：返回收银台地址与表单参数")
    void createReturnsRedirectParams() {
        Payment payment = new Payment();
        payment.setPayNo("SR1");
        PayCreateResult result = new PayCreateResult();
        result.setPayment(payment);
        result.setRedirectUrl("https://pay.linktrust-pay.com/webpay/payform");
        result.setRedirectParams(Map.of("merId", "M810000022", "merOrderId", "SR1", "amount", "15000"));
        when(linkTrustPayChannel.createPayment(booking, 2)).thenReturn(result);

        LinkTrustPayVO vo = controller.create(req(2)).getData();
        assertEquals("https://pay.linktrust-pay.com/webpay/payform", vo.getPayUrl());
        assertEquals("M810000022", vo.getMerId());
        assertEquals("SR1", vo.getMerOrderId());
        assertEquals("15000", vo.getAmount());
    }

    @Test
    @DisplayName("发起支付：未选方式/未启用/非本人订单/非待支付 均拒绝")
    void createGuards() {
        assertThrows(BusinessException.class, () -> controller.create(req(null)));

        AuthContext.set(6L);
        assertThrows(BusinessException.class, () -> controller.create(req(1)));
        AuthContext.set(5L);

        booking.setPayStatus(1);
        assertThrows(BusinessException.class, () -> controller.create(req(1)));
        booking.setPayStatus(0);

        LinkTrustPaymentController disabled = new LinkTrustPaymentController(bookingService,
                paymentService, new LinkTrustConfig("", "u", "q", "wechatpay", "jpy"),
                linkTrustPayChannel, paymentSyncService);
        assertThrows(BusinessException.class, () -> disabled.create(req(1)));
    }

    @Test
    @DisplayName("回调：渠道未启用返回503（配置就绪前让渠道重试）")
    void callbackDisabledReturns503() {
        LinkTrustPaymentController disabled = new LinkTrustPaymentController(bookingService,
                paymentService, new LinkTrustConfig("", "u", "q", "wechatpay", "jpy"),
                linkTrustPayChannel, paymentSyncService);
        ResponseEntity<String> resp = disabled.callback(ipn("M810000022", "SR1", "success"));
        assertEquals(503, resp.getStatusCode().value());
    }

    @Test
    @DisplayName("回调：merId 不匹配 → 200 忽略（停止无效重发）")
    void callbackWrongMerIdIgnored() {
        ResponseEntity<String> resp = controller.callback(ipn("M999999999", "SR1", "success"));
        assertEquals(200, resp.getStatusCode().value());
        verify(paymentSyncService, never()).applyLinkTrustTxn(any(), any());
    }

    @Test
    @DisplayName("回调：找不到对应支付流水 → 200 忽略")
    void callbackUnknownOrderIgnored() {
        when(paymentQuery.one()).thenReturn(null);
        ResponseEntity<String> resp = controller.callback(ipn("M810000022", "SRX", "success"));
        assertEquals(200, resp.getStatusCode().value());
        verify(paymentSyncService, never()).applyLinkTrustTxn(any(), any());
    }

    @Test
    @DisplayName("回调：已成功的支付幂等短路 → 200，不再反查")
    void callbackIdempotentWhenAlreadyPaid() throws Exception {
        Payment paid = new Payment();
        paid.setPayNo("SR1");
        paid.setPayStatus(1);
        when(paymentQuery.one()).thenReturn(paid);
        ResponseEntity<String> resp = controller.callback(ipn("M810000022", "SR1", "success"));
        assertEquals(200, resp.getStatusCode().value());
        verify(linkTrustPayChannel, never()).query(any());
    }

    @Test
    @DisplayName("回调：不信任 IPN 内容，反查 transactions 后落库")
    void callbackVerifiesViaQuery() throws Exception {
        Payment pending = new Payment();
        pending.setPayNo("SR1");
        pending.setPayStatus(0);
        when(paymentQuery.one()).thenReturn(pending);
        LinkTrustTxn txn = new LinkTrustTxn();
        txn.setResult("success");
        when(linkTrustPayChannel.query(pending)).thenReturn(txn);

        // IPN 声称 failure，也以反查结果为准
        ResponseEntity<String> resp = controller.callback(ipn("M810000022", "SR1", "failure"));
        assertEquals(200, resp.getStatusCode().value());
        verify(paymentSyncService).applyLinkTrustTxn(pending, txn);
    }

    @Test
    @DisplayName("回调：反查网络失败 → 500（渠道一小时后重发）")
    void callbackQueryFailureReturns500() throws Exception {
        Payment pending = new Payment();
        pending.setPayNo("SR1");
        pending.setPayStatus(0);
        when(paymentQuery.one()).thenReturn(pending);
        when(linkTrustPayChannel.query(pending)).thenThrow(new IllegalStateException("timeout"));

        ResponseEntity<String> resp = controller.callback(ipn("M810000022", "SR1", "success"));
        assertEquals(500, resp.getStatusCode().value());
        verify(paymentSyncService, never()).applyLinkTrustTxn(any(), any());
    }
}
