package com.seatech.minsu.service;

import com.seatech.minsu.config.LinkTrustConfig;
import com.seatech.minsu.config.StripeConfig;
import com.seatech.minsu.dto.LinkTrustTxn;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.service.impl.LinkTrustClient;
import com.seatech.minsu.service.impl.LinkTrustPayChannel;
import com.seatech.minsu.service.impl.StripePayChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 渠道状态同步：以渠道侧结果为准落库待支付 payment。
 * 结果页兜底轮询 / 待支付超时 Job / LinkTrust IPN 回调三方共用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncService {

    private final StripePayChannel stripePayChannel;
    private final LinkTrustPayChannel linkTrustPayChannel;
    private final StripeConfig stripeConfig;
    private final LinkTrustConfig linkTrustConfig;
    private final PaymentStateService paymentStateService;

    /**
     * 同步一次渠道侧状态并落库。
     *
     * @return 渠道侧认定状态：0处理中/未知 1成功 2失败（非待支付行直接返回本地状态）
     */
    public int sync(Payment payment) {
        if (payment == null) {
            return 0;
        }
        if (payment.getPayStatus() == null || payment.getPayStatus() != 0) {
            return payment.getPayStatus() == null ? 0 : payment.getPayStatus();
        }
        Integer channel = payment.getChannel();
        if (channel != null && channel == 2 && stripeConfig.isEnabled()) {
            int status = stripePayChannel.queryStatus(payment);
            if (status == 1) {
                paymentStateService.markSucceeded(payment, stripePayChannel.latestChargeOf(payment));
            } else if (status == 2) {
                paymentStateService.markFailed(payment);
            }
            return status;
        }
        if (channel != null && channel == 3 && linkTrustConfig.isEnabled()) {
            try {
                return applyLinkTrustTxn(payment, linkTrustPayChannel.query(payment));
            } catch (Exception e) {
                log.warn("LinkTrust 状态同步失败: payNo={}", payment.getPayNo(), e);
                return 0;
            }
        }
        return 0;
    }

    /**
     * 以 LinkTrust 反查结果为准落库（IPN 回调与轮询共用）。
     * 金额不符时拒绝落成功、保持待支付，留待人工排查（返回值仍为渠道认定状态）。
     *
     * @return 渠道认定状态：0处理中 1成功 2失败
     */
    public int applyLinkTrustTxn(Payment payment, LinkTrustTxn txn) {
        if (txn == null) {
            return 0;
        }
        int status = LinkTrustClient.mapResultStatus(txn.getResult());
        if (status == 1) {
            long expected = linkTrustConfig.toChannelAmount(payment.getAmount());
            if (txn.getAmount() != null && txn.getAmount() != expected) {
                log.error("LinkTrust 金额不符，拒绝落账: payNo={}, 渠道金额={}, 应收={}",
                        payment.getPayNo(), txn.getAmount(), expected);
                return status;
            }
            // 用户在收银台实际选择的方式以渠道返回为准
            Integer methodCode = LinkTrustClient.mapPayMethodCode(txn.getPayMethod());
            if (methodCode != null) {
                payment.setPayMethod(methodCode);
            }
            paymentStateService.markSucceeded(payment, txn.getTransactionId());
        } else if (status == 2) {
            paymentStateService.markFailed(payment);
        }
        return status;
    }
}
