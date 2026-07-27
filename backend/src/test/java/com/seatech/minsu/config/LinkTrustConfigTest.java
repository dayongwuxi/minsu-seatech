package com.seatech.minsu.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** LinkTrust 配置：启用判定、方式解析、金额取整（日元整数） */
class LinkTrustConfigTest {

    private LinkTrustConfig config(String merId, String methods, String currency) {
        // 供应商服务器仅开 HTTP 80（443 不通，2026-07-24 实测），默认 URL 必须 http
        return new LinkTrustConfig(merId,
                "http://pay.linktrust-pay.com/webpay/payform",
                "http://pay.linktrust-pay.com/webpay/transactions",
                methods, currency);
    }

    @Test
    @DisplayName("merId 为空即渠道禁用")
    void disabledWithoutMerId() {
        assertFalse(config("", "wechatpay", "jpy").isEnabled());
        assertFalse(config("   ", "wechatpay", "jpy").isEnabled());
        assertTrue(config("M810000022", "wechatpay", "jpy").isEnabled());
    }

    @Test
    @DisplayName("merId 前后空白被裁剪")
    void trimsMerId() {
        assertEquals("M810000022", config(" M810000022 ", "wechatpay", "jpy").getMerId());
    }

    @Test
    @DisplayName("支付方式逗号解析，空白项过滤，空串回退默认三方式")
    void parsesMethodList() {
        assertEquals(List.of("wechatpay", "alipay", "unionpay"),
                config("M1", "wechatpay, alipay , unionpay", "jpy").getMethodList());
        assertEquals(List.of("alipay"), config("M1", "alipay,,  ", "jpy").getMethodList());
        assertEquals(List.of("wechatpay", "alipay", "unionpay"),
                config("M1", "", "jpy").getMethodList());
    }

    @Test
    @DisplayName("金额转渠道整数：四舍五入")
    void toChannelAmountRounds() {
        LinkTrustConfig c = config("M1", "wechatpay", "jpy");
        assertEquals(15000L, c.toChannelAmount(new BigDecimal("15000.00")));
        assertEquals(101L, c.toChannelAmount(new BigDecimal("100.50")));
        assertEquals(100L, c.toChannelAmount(new BigDecimal("100.49")));
        assertEquals(1L, c.toChannelAmount(new BigDecimal("0.51")));
    }
}
