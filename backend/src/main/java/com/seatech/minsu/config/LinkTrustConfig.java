package com.seatech.minsu.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LinkTrust Pay 配置（环境变量注入，缺省即渠道禁用）。
 * LINKTRUST_MER_ID(店铺ID) / LINKTRUST_PAYFORM_URL / LINKTRUST_QUERY_URL /
 * LINKTRUST_PAYMENT_METHODS(逗号分隔, 默认 wechatpay,alipay,unionpay)
 * 供应商规约：HTML FORM POST 跳转收银台；IPN 无签名，落库前必须反查 transactions。
 */
@Slf4j
@Getter
@Component
public class LinkTrustConfig {

    private static final List<String> DEFAULT_METHODS = List.of("wechatpay", "alipay", "unionpay");

    private final String merId;
    private final String payformUrl;
    private final String queryUrl;
    private final List<String> methodList;
    private final String siteCurrency;

    // 供应商服务器仅开 HTTP 80（443 不通，2026-07-24 实测），默认 URL 用 http
    public LinkTrustConfig(@Value("${LINKTRUST_MER_ID:}") String merId,
                           @Value("${LINKTRUST_PAYFORM_URL:http://pay.linktrust-pay.com/webpay/payform}") String payformUrl,
                           @Value("${LINKTRUST_QUERY_URL:http://pay.linktrust-pay.com/webpay/transactions}") String queryUrl,
                           @Value("${LINKTRUST_PAYMENT_METHODS:wechatpay,alipay,unionpay}") String paymentMethods,
                           @Value("${PAYMENT_CURRENCY:${STRIPE_CURRENCY:cny}}") String siteCurrency) {
        this.merId = merId == null ? "" : merId.trim();
        this.payformUrl = payformUrl;
        this.queryUrl = queryUrl;
        List<String> methods = Arrays.stream(
                        (paymentMethods == null ? "" : paymentMethods).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        this.methodList = methods.isEmpty() ? DEFAULT_METHODS : methods;
        this.siteCurrency = StringUtils.hasText(siteCurrency) ? siteCurrency.trim().toLowerCase() : "cny";
    }

    @PostConstruct
    void init() {
        // LinkTrust 金额单位为日元整数：站点结算货币非 jpy 时金额将按数值取整直传，存在币种错配风险
        if (isEnabled() && !"jpy".equals(siteCurrency)) {
            log.warn("LinkTrust 渠道已启用但站点结算货币为 {}（LinkTrust 仅收日元整数），请确认 PAYMENT_CURRENCY=jpy", siteCurrency);
        }
    }

    public boolean isEnabled() {
        return StringUtils.hasText(merId);
    }

    /** LinkTrust amount 参数为日元整数：四舍五入取整 */
    public long toChannelAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
