package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * LinkTrust 支付端点：
 * create 返回收银台表单参数，前端隐藏表单 POST 跳转（新标签页）+ 本页转结果页轮询；
 * callback 为 IPN 通知（免登录、表单编码）。规约无签名机制 → 不信任 IPN 内容，
 * 一律服务端反查 /webpay/transactions 为准；未回 200 时渠道每小时重发。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LinkTrustPaymentController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final LinkTrustConfig linkTrustConfig;
    private final LinkTrustPayChannel linkTrustPayChannel;
    private final PaymentSyncService paymentSyncService;

    /** 发起 LinkTrust 支付：创建/复用待支付流水，返回收银台跳转参数 */
    @PostMapping("/api/payments/linktrust/create")
    public Result<LinkTrustPayVO> create(@RequestBody LinkTrustPayRequest req) {
        if (!linkTrustConfig.isEnabled()) {
            throw new BusinessException("LinkTrust 支付未启用");
        }
        if (req.getPayMethod() == null) {
            throw new BusinessException("请选择支付方式");
        }
        Booking booking = getOwnedBooking(req.getOrderNo());
        requireWaitPay(booking);
        PayCreateResult result = linkTrustPayChannel.createPayment(booking, req.getPayMethod());
        LinkTrustPayVO vo = new LinkTrustPayVO();
        vo.setPayUrl(result.getRedirectUrl());
        vo.setMerId(result.getRedirectParams().get("merId"));
        vo.setMerOrderId(result.getRedirectParams().get("merOrderId"));
        vo.setAmount(result.getRedirectParams().get("amount"));
        vo.setPayNo(result.getPayment().getPayNo());
        return Result.ok(vo);
    }

    /** IPN 回调（免登录）：反查确认后落库；200 停止重发，500 触发渠道一小时后重发 */
    @PostMapping("/api/payments/linktrust/callback")
    public ResponseEntity<String> callback(@RequestParam Map<String, String> params) {
        if (!linkTrustConfig.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("linktrust disabled");
        }
        String merId = params.get("merId");
        String merOrderId = params.get("merOrderId");
        if (!linkTrustConfig.getMerId().equals(merId) || !StringUtils.hasText(merOrderId)) {
            log.warn("LinkTrust 回调 merId 不匹配或缺少 merOrderId, 忽略: merId={}, merOrderId={}", merId, merOrderId);
            return ResponseEntity.ok("ignored");
        }
        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getPayNo, merOrderId)
                .eq(Payment::getChannel, 3)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
        if (payment == null) {
            log.warn("LinkTrust 回调找不到支付流水, 忽略: merOrderId={}", merOrderId);
            return ResponseEntity.ok("ignored");
        }
        if (payment.getPayStatus() != null && payment.getPayStatus() == 1) {
            return ResponseEntity.ok("success");
        }
        LinkTrustTxn txn;
        try {
            txn = linkTrustPayChannel.query(payment);
        } catch (Exception e) {
            log.warn("LinkTrust 回调反查失败, 等待渠道重发: merOrderId={}", merOrderId, e);
            return ResponseEntity.internalServerError().body("query failed");
        }
        paymentSyncService.applyLinkTrustTxn(payment, txn);
        return ResponseEntity.ok("success");
    }

    private void requireWaitPay(Booking booking) {
        if (booking.getPayStatus() != 0) {
            throw new BusinessException("订单已支付或已退款");
        }
        if (booking.getBookingStatus() == 4) {
            throw new BusinessException("订单已取消，无法支付");
        }
    }

    private Booking getOwnedBooking(String orderNo) {
        Booking booking = bookingService.lambdaQuery()
                .eq(Booking::getOrderNo, orderNo)
                .one();
        if (booking == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!booking.getMemberId().equals(AuthContext.get())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该订单");
        }
        return booking;
    }
}
