package com.seatech.minsu.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stripe 配置（环境变量注入，缺省即 Mock 模式）。
 * STRIPE_SECRET_KEY / STRIPE_PUBLISHABLE_KEY / STRIPE_WEBHOOK_SECRET /
 * PAYMENT_CURRENCY(站点结算货币, 历史名 STRIPE_CURRENCY 兜底, 默认 cny) /
 * STRIPE_PAYMENT_METHODS(逗号分隔, 默认 card)
 */
@Getter
@Component
public class StripeConfig {

    /** Stripe 零小数货币(金额不 ×100) */
    private static final Set<String> ZERO_DECIMAL = Set.of(
            "bif", "clp", "djf", "gnf", "jpy", "kmf", "krw", "mga",
            "pyg", "rwf", "ugx", "vnd", "vuv", "xaf", "xof", "xpf");

    private final String secretKey;
    private final String publishableKey;
    private final String webhookSecret;
    private final String currency;
    private final List<String> methodList;

    public StripeConfig(@Value("${STRIPE_SECRET_KEY:}") String secretKey,
                        @Value("${STRIPE_PUBLISHABLE_KEY:}") String publishableKey,
                        @Value("${STRIPE_WEBHOOK_SECRET:}") String webhookSecret,
                        @Value("${PAYMENT_CURRENCY:${STRIPE_CURRENCY:cny}}") String currency,
                        @Value("${STRIPE_PAYMENT_METHODS:card}") String paymentMethods) {
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.publishableKey = publishableKey == null ? "" : publishableKey.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.currency = StringUtils.hasText(currency) ? currency.trim().toLowerCase() : "cny";
        List<String> methods = Arrays.stream(
                        (paymentMethods == null ? "" : paymentMethods).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        this.methodList = methods.isEmpty() ? List.of("card") : methods;
    }

    @PostConstruct
    void init() {
        if (isEnabled()) {
            Stripe.apiKey = secretKey;
        }
    }

    public boolean isEnabled() {
        return StringUtils.hasText(secretKey);
    }

    /** 站点结算货币是否为零小数货币（jpy/krw 等，金额只有整数位） */
    public boolean isZeroDecimalCurrency() {
        return ZERO_DECIMAL.contains(currency);
    }

    /** 金额换算为 Stripe 最小货币单位：零小数货币不乘100，其余×100 */
    public long toMinorUnits(BigDecimal amount) {
        if (ZERO_DECIMAL.contains(currency)) {
            return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
