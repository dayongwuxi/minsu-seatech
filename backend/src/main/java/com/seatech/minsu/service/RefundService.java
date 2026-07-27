package com.seatech.minsu.service;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 统一退款服务（状态机见 docs/支付促销退款设计.md 第3节、docs/LinkTrustPay接入设计.md 第4节）。
 * 按渠道 RefundMode 分支：
 * INSTANT(Mock) 同意即置3；API_ASYNC(Stripe) 调渠道退款→1退款中→webhook 置3；
 * MANUAL_OFFLINE(LinkTrust) 同意仅置1，管理员渠道后台手工退款后 complete 置3；拒绝→2
 * status=3 联动: payment.pay_status=3、booking.pay_status=2、未入住取消场景 booking_status=4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRecordService refundRecordService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final PayChannelRegistry channelRegistry;

    /** 用户发起退款申请（取消已支付订单 / 已完成订单申请退款） */
    public RefundRecord createApply(Booking booking, String reason) {
        Payment latest = latestPayment(booking.getId());
        RefundRecord record = new RefundRecord();
        record.setBookingId(booking.getId());
        record.setMemberId(booking.getMemberId());
        record.setRefundAmount(booking.getTotalAmount());
        record.setReason(reason);
        record.setStatus(0);
        record.setChannel(latest == null || latest.getChannel() == null ? 1 : latest.getChannel());
        refundRecordService.save(record);
        return record;
    }

    /** 管理端同意退款：按渠道退款模式进入对应分支 */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long refundId, Long adminId) {
        RefundRecord record = requireRecord(refundId);
        if (record.getStatus() == null || record.getStatus() != 0) {
            throw new BusinessException("该退款申请已处理");
        }
        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getBookingId, record.getBookingId())
                .eq(Payment::getPayStatus, 1)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
        record.setHandlerId(adminId);
        record.setHandleTime(LocalDateTime.now());
        PayChannel channel = channelRegistry.byCode(record.getChannel());
        PayChannel.RefundMode mode = channel == null
                ? PayChannel.RefundMode.INSTANT : channel.refundMode();
        if (mode == PayChannel.RefundMode.MANUAL_OFFLINE) {
            // 渠道无退款 API：置退款中，管理员渠道后台手工退款后调 complete 确认到账
            record.setStatus(1);
            refundRecordService.updateById(record);
        } else if (mode == PayChannel.RefundMode.API_ASYNC
                && payment != null && channel.canRefund(payment)) {
            // 渠道退款异步到账：先置1退款中，webhook 收到成功事件后置3
            String channelRefundId = channel.refund(payment, record);
            record.setStripeRefundId(channelRefundId);
            record.setStatus(1);
            refundRecordService.updateById(record);
        } else {
            refundRecordService.updateById(record);
            markRefunded(record);
        }
    }

    /** 管理端拒绝退款 */
    public void reject(Long refundId, Long adminId, String reason) {
        RefundRecord record = requireRecord(refundId);
        if (record.getStatus() == null || record.getStatus() != 0) {
            throw new BusinessException("该退款申请已处理");
        }
        record.setStatus(2);
        record.setHandlerId(adminId);
        record.setHandleTime(LocalDateTime.now());
        if (StringUtils.hasText(reason)) {
            String base = record.getReason() == null ? "" : record.getReason();
            record.setReason(base + "；拒绝原因: " + reason);
        }
        refundRecordService.updateById(record);
    }

    /** 管理端确认线下退款完成（仅 MANUAL_OFFLINE 渠道的退款中记录） */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long refundId, Long adminId) {
        RefundRecord record = requireRecord(refundId);
        if (record.getStatus() == null || record.getStatus() != 1) {
            throw new BusinessException("该退款申请不在退款中状态");
        }
        if (channelRegistry.refundModeOf(record.getChannel()) != PayChannel.RefundMode.MANUAL_OFFLINE) {
            throw new BusinessException("该渠道退款由支付渠道回调自动确认，无需手工完成");
        }
        if (record.getHandlerId() == null) {
            record.setHandlerId(adminId);
        }
        markRefunded(record);
    }

    /**
     * 管理端在订单页直接退款：复用申请中的记录，无则代建后走 approve。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDirectRefund(Booking booking, Long adminId) {
        if (booking.getPayStatus() == null || booking.getPayStatus() != 1) {
            throw new BusinessException("订单未支付或已退款");
        }
        RefundRecord existing = refundRecordService.lambdaQuery()
                .eq(RefundRecord::getBookingId, booking.getId())
                .in(RefundRecord::getStatus, 0, 1)
                .orderByDesc(RefundRecord::getId)
                .last("LIMIT 1")
                .one();
        if (existing != null && existing.getStatus() == 1) {
            throw new BusinessException("退款处理中，请在退款管理中完成处理");
        }
        if (existing == null) {
            existing = createApply(booking, "管理员发起退款");
        }
        approve(existing.getId(), adminId);
    }

    /** 退款到账（webhook / 线下确认 / mock 即时）：置3并联动 payment/booking */
    @Transactional(rollbackFor = Exception.class)
    public void markRefunded(RefundRecord record) {
        if (record.getStatus() != null && record.getStatus() == 3) {
            return;
        }
        record.setStatus(3);
        if (record.getHandleTime() == null) {
            record.setHandleTime(LocalDateTime.now());
        }
        refundRecordService.updateById(record);

        paymentService.lambdaUpdate()
                .eq(Payment::getBookingId, record.getBookingId())
                .eq(Payment::getPayStatus, 1)
                .set(Payment::getPayStatus, 3)
                .update();

        Booking booking = bookingService.getById(record.getBookingId());
        if (booking != null) {
            booking.setPayStatus(2);
            // 未入住取消场景置已取消；已入住/已完成订单保持原状态
            if (booking.getBookingStatus() != null
                    && (booking.getBookingStatus() == 0 || booking.getBookingStatus() == 1)) {
                booking.setBookingStatus(4);
            }
            bookingService.updateById(booking);
        }
        log.info("退款完成: refundRecordId={}, bookingId={}", record.getId(), record.getBookingId());
    }

    /** webhook: refund.updated(status=succeeded) 按 Stripe Refund id 定位 */
    public void markRefundedByStripeRefundId(String stripeRefundId) {
        if (!StringUtils.hasText(stripeRefundId)) {
            return;
        }
        RefundRecord record = refundRecordService.lambdaQuery()
                .eq(RefundRecord::getStripeRefundId, stripeRefundId)
                .one();
        if (record != null) {
            markRefunded(record);
        }
    }

    /** webhook: charge.refunded 按 PaymentIntent id 定位 */
    public void markRefundedByIntentId(String paymentIntentId) {
        if (!StringUtils.hasText(paymentIntentId)) {
            return;
        }
        Payment payment = paymentService.lambdaQuery()
                .eq(Payment::getStripePaymentIntentId, paymentIntentId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
        if (payment == null) {
            return;
        }
        RefundRecord record = refundRecordService.lambdaQuery()
                .eq(RefundRecord::getBookingId, payment.getBookingId())
                .in(RefundRecord::getStatus, 0, 1)
                .orderByDesc(RefundRecord::getId)
                .last("LIMIT 1")
                .one();
        if (record != null) {
            markRefunded(record);
        }
    }

    private Payment latestPayment(Long bookingId) {
        return paymentService.lambdaQuery()
                .eq(Payment::getBookingId, bookingId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1")
                .one();
    }

    private RefundRecord requireRecord(Long id) {
        RefundRecord record = refundRecordService.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款申请不存在");
        }
        return record;
    }
}
