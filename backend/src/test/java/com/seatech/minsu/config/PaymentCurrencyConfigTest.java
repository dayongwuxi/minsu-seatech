package com.seatech.minsu.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 站点结算货币配置契约：中性变量 PAYMENT_CURRENCY 优先，历史变量 STRIPE_CURRENCY 兜底（线上 .env 不断），
 * 最终缺省 cny。StripeConfig 与 LinkTrustConfig 必须读同一占位符链，防止两处货币配置不一致。
 */
class PaymentCurrencyConfigTest {

    private static final String EXPECTED = "${PAYMENT_CURRENCY:${STRIPE_CURRENCY:cny}}";

    private static List<String> valueAnnotations(Class<?> configClass) {
        List<String> values = new ArrayList<>();
        for (Constructor<?> ctor : configClass.getDeclaredConstructors()) {
            for (Parameter p : ctor.getParameters()) {
                Value v = p.getAnnotation(Value.class);
                if (v != null) {
                    values.add(v.value());
                }
            }
        }
        return values;
    }

    @Test
    @DisplayName("StripeConfig 货币读 PAYMENT_CURRENCY，STRIPE_CURRENCY 兜底")
    void stripeConfigUsesNeutralCurrencyVar() {
        assertTrue(valueAnnotations(StripeConfig.class).contains(EXPECTED),
                "StripeConfig 应使用 " + EXPECTED);
    }

    @Test
    @DisplayName("LinkTrustConfig 站点货币与 StripeConfig 同源")
    void linkTrustConfigUsesSameCurrencyVar() {
        assertTrue(valueAnnotations(LinkTrustConfig.class).contains(EXPECTED),
                "LinkTrustConfig 应使用 " + EXPECTED);
    }
}
