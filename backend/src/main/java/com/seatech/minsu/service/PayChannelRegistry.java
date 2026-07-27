package com.seatech.minsu.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 支付渠道注册表：收集全部 PayChannel bean 按编码路由，新渠道零改动接入 */
@Component
public class PayChannelRegistry {

    private final Map<Integer, PayChannel> channels;

    public PayChannelRegistry(List<PayChannel> beans) {
        this.channels = beans.stream()
                .collect(Collectors.toUnmodifiableMap(PayChannel::channel, Function.identity()));
    }

    /** 按渠道编码取实现；未知返回 null */
    public PayChannel byCode(Integer channel) {
        return channel == null ? null : channels.get(channel);
    }

    /** 渠道退款模式；未知渠道回退 INSTANT（兼容历史 mock 单） */
    public PayChannel.RefundMode refundModeOf(Integer channel) {
        PayChannel ch = byCode(channel);
        return ch == null ? PayChannel.RefundMode.INSTANT : ch.refundMode();
    }
}
