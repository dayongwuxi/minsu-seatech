package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.dto.PayRequest;
import com.seatech.minsu.dto.PaymentConfigVO;
import com.seatech.minsu.dto.PaymentResultVO;
import com.seatech.minsu.dto.StripeIntentRequest;
import com.seatech.minsu.dto.StripeIntentVO;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.MemberPaymentMethod;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.PaymentMethodService;
import com.seatech.minsu.service.PaymentService;
import com.seatech.minsu.service.PaymentStateService;
import com.seatech.minsu.service.PaymentSyncService;
import com.seatech.minsu.service.RefundService;
import com.seatech.minsu.service.RoomService;
import com.seatech.minsu.service.impl.LinkTrustClient;
import com.seatech.minsu.service.impl.MockPayChannel;
import com.seatech.minsu.service.impl.StripePayChannel;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 会员端支付：Stripe PaymentIntents 聚合 + Mock 降级。
 * webhook 为主、结果页主动查询为兜底（双保险）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final MemberService memberService;
    private final PaymentMethodService paymentMethodService;
    private final RoomService roomService;
    private final StripeConfig stripeConfig;
    private final LinkTrustConfig linkTrustConfig;
    private final MockPayChannel mockPayChannel;
    private final StripePayChannel stripePayChannel;
    private final PaymentStateService paymentStateService;
    private final PaymentSyncService paymentSyncService;
    private final RefundService refundService;

    /** 支付配置（免登录，前端据此决定渲染 Stripe Payment Element 还是模拟支付） */
    @GetMapping("/api/payments/config")
    public Result<PaymentConfigVO> config() {
        PaymentConfigVO vo = new PaymentConfigVO();
        vo.setStripeEnabled(stripeConfig.isEnabled());
        vo.setPublishableKey(stripeConfig.getPublishableKey());
        vo.setMethods(stripeConfig.getMethodList());
        vo.setCurrency(stripeConfig.getCurrency());
        vo.setLinktrustEnabled(linkTrustConfig.isEnabled());
        vo.setLinktrustMethods(linkTrustConfig.getMethodList().stream()
                .map(LinkTrustClient::mapPayMethodCode)
                .filter(Objects::nonNull)
                .toList());
        return Result.ok(vo);
    }

    /** 模拟支付（Stripe 未启用时的降级通道；传 paymentMethodId 记录 pay_method=3） */
    @PostMapping("/api/payments")
    public Result<PaymentResultVO> pay(@RequestBody PayRequest req) {
        Integer payMethod = req.getPayMethod();
        if (req.getPaymentMethodId() != null) {
            paymentMethodService.requireOwned(AuthContext.get(), req.getPaymentMethodId());
            payMethod = 3;
        } else if (payMethod == null) {
            throw new BusinessException("请选择支付方式");
        }
        Booking booking = getOwnedBooking(req.getOrderNo());
        requireWaitPay(booking);
        PayCreateResult result = mockPayChannel.createPayment(booking, payMethod);
        return Result.ok(buildResult(booking, result.getPayment()));
    }

    /** 创建/复用 Stripe PaymentIntent */
    @PostMapping("/api/payments/stripe/intent")
    public Result<StripeIntentVO> stripeIntent(@RequestBody StripeIntentRequest req) {
        if (!stripeConfig.isEnabled()) {
            throw new BusinessException("Stripe 支付未启用");
        }
        Booking booking = getOwnedBooking(req.getOrderNo());
        requireWaitPay(booking);
        MemberPaymentMethod saved = null;
        String customerId = null;
        if (req.getPaymentMethodId() != null) {
            saved = paymentMethodService.requireOwned(AuthContext.get(), req.getPaymentMethodId());
            if (saved.getChannel() == null || saved.getChannel() != 2
                    || !StringUtils.hasText(saved.getStripePaymentMethodId())) {
                throw new BusinessException("该支付方式不支持在线支付渠道");
            }
            Member member = memberService.getById(booking.getMemberId());
            customerId = member == null ? null : member.getStripeCustomerId();
            if (!StringUtils.hasText(customerId)) {
                throw new BusinessException("该支付方式不可用，请重新绑卡");
            }
        }
        PayCreateResult result = stripePayChannel.createPayment(booking, customerId, saved);
        StripeIntentVO vo = new StripeIntentVO();
        vo.setClientSecret(result.getClientSecret());
        vo.setPaymentIntentId(result.getPaymentIntentId());
        vo.setAmount(result.getAmount());
        vo.setCurrency(result.getCurrency());
        vo.setRequiresAction(result.isRequiresAction());
        vo.setSucceeded(result.isSucceeded());
        return Result.ok(vo);
    }

    /** Stripe Webhook（免登录，签名验签；未配置 STRIPE_WEBHOOK_SECRET 时拒绝） */
    @PostMapping("/api/payments/stripe/webhook")
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!StringUtils.hasText(stripeConfig.getWebhookSecret())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("webhook secret not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook 验签失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        } catch (Exception e) {
            log.warn("Stripe webhook 解析失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid payload");
        }
        StripeObject data = event.getDataObjectDeserializer().getObject().orElse(null);
        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    if (data instanceof PaymentIntent intent) {
                        Payment payment = locatePayment(intent);
                        if (payment != null) {
                            paymentStateService.markSucceeded(payment, intent.getLatestCharge());
                        }
                    }
                }
                case "payment_intent.payment_failed" -> {
                    if (data instanceof PaymentIntent intent) {
                        Payment payment = locatePayment(intent);
                        if (payment != null) {
                            paymentStateService.markFailed(payment);
                        }
                    }
                }
                case "refund.updated" -> {
                    if (data instanceof Refund refund && "succeeded".equals(refund.getStatus())) {
                        refundService.markRefundedByStripeRefundId(refund.getId());
                    }
                }
                case "charge.refunded" -> {
                    if (data instanceof Charge charge) {
                        refundService.markRefundedByIntentId(charge.getPaymentIntent());
                    }
                }
                default -> log.debug("忽略 Stripe 事件: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("处理 Stripe 事件失败: type={}", event.getType(), e);
            return ResponseEntity.internalServerError().body("handler error");
        }
        return ResponseEntity.ok("ok");
    }

    /** 支付结果（外部渠道单在回调未达时主动查询渠道同步一次，Stripe/LinkTrust 通用） */
    @GetMapping("/api/payments/result")
    public Result<PaymentResultVO> result(@RequestParam String orderNo) {
        Booking booking = getOwnedBooking(orderNo);
        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getBookingId, booking.getId())
                .orderByDesc(Payment::getId)
                .list().stream().findFirst().orElse(null);
        if (payment != null && payment.getPayStatus() != null && payment.getPayStatus() == 0
                && paymentSyncService.sync(payment) == 1) {
            booking = bookingService.getById(booking.getId());
        }
        return Result.ok(buildResult(booking, payment));
    }

    private Payment locatePayment(PaymentIntent intent) {
        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getStripePaymentIntentId, intent.getId())
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
        if (payment != null) {
            return payment;
        }
        // 兜底：按 metadata.orderNo 定位
        String orderNo = intent.getMetadata() == null ? null : intent.getMetadata().get("orderNo");
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        Booking booking = bookingService.lambdaQuery().eq(Booking::getOrderNo, orderNo).one();
        if (booking == null) {
            return null;
        }
        return paymentService.lambdaQuery()
                .eq(Payment::getBookingId, booking.getId())
                .eq(Payment::getChannel, 2)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
    }

    private void requireWaitPay(Booking booking) {
        if (booking.getPayStatus() != 0) {
            throw new BusinessException("订单已支付或已退款");
        }
        if (booking.getBookingStatus() == 4) {
            throw new BusinessException("订单已取消，无法支付");
        }
    }

    private PaymentResultVO buildResult(Booking booking, Payment payment) {
        PaymentResultVO vo = new PaymentResultVO();
        vo.setOrderNo(booking.getOrderNo());
        vo.setCheckinDate(booking.getCheckinDate());
        vo.setCheckoutDate(booking.getCheckoutDate());
        vo.setNights(booking.getNights());
        vo.setAmount(booking.getTotalAmount());
        vo.setCurrency(booking.getCurrency());
        Room room = roomService.getById(booking.getRoomId());
        vo.setRoomName(room == null ? null : room.getRoomName());
        if (payment != null) {
            vo.setPayNo(payment.getPayNo());
            vo.setPayMethod(payment.getPayMethod());
            vo.setChannel(payment.getChannel());
            vo.setPayStatus(payment.getPayStatus());
            vo.setPayTime(payment.getPayTime());
            vo.setAmount(payment.getAmount());
            vo.setCurrency(payment.getCurrency());
        }
        return vo;
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
