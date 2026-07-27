package com.seatech.minsu.service;

import com.seatech.minsu.dto.PayCreateResult;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 渠道注册表：按编码路由，未知渠道退款模式回退 INSTANT（兼容历史 mock 单） */
class PayChannelRegistryTest {

    private static PayChannel channel(int code, PayChannel.RefundMode mode) {
        return new PayChannel() {
            @Override
            public int channel() {
                return code;
            }

            @Override
            public PayChannel.RefundMode refundMode() {
                return mode;
            }

            @Override
            public PayCreateResult createPayment(Booking booking, Integer payMethod) {
                return null;
            }

            @Override
            public String refund(Payment payment, RefundRecord record) {
                return null;
            }

            @Override
            public int queryStatus(Payment payment) {
                return 0;
            }
        };
    }

    @Test
    @DisplayName("按渠道编码路由到对应实现")
    void routesByCode() {
        PayChannel mock = channel(1, PayChannel.RefundMode.INSTANT);
        PayChannel stripe = channel(2, PayChannel.RefundMode.API_ASYNC);
        PayChannel linktrust = channel(3, PayChannel.RefundMode.MANUAL_OFFLINE);
        PayChannelRegistry registry = new PayChannelRegistry(List.of(mock, stripe, linktrust));

        assertSame(stripe, registry.byCode(2));
        assertSame(linktrust, registry.byCode(3));
        assertNull(registry.byCode(99));
        assertNull(registry.byCode(null));
    }

    @Test
    @DisplayName("退款模式：未知/空渠道回退 INSTANT")
    void refundModeFallsBackToInstant() {
        PayChannelRegistry registry = new PayChannelRegistry(
                List.of(channel(3, PayChannel.RefundMode.MANUAL_OFFLINE)));
        assertEquals(PayChannel.RefundMode.MANUAL_OFFLINE, registry.refundModeOf(3));
        assertEquals(PayChannel.RefundMode.INSTANT, registry.refundModeOf(99));
        assertEquals(PayChannel.RefundMode.INSTANT, registry.refundModeOf(null));
    }
}
