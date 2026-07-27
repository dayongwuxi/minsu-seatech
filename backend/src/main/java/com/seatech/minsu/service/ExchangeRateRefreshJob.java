package com.seatech.minsu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/** 汇率定时刷新：启动异步刷新一次 + 每 6 小时定时刷新 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateRefreshJob implements ApplicationRunner {

    private static final long SIX_HOURS_MS = 6 * 60 * 60 * 1000L;

    private final ExchangeRateService exchangeRateService;

    /** 启动时异步刷新一次，不阻塞启动 */
    @Override
    public void run(ApplicationArguments args) {
        CompletableFuture.runAsync(() -> {
            int updated = exchangeRateService.fetchFromApi();
            log.info("启动汇率刷新: 更新 {} 条", updated);
        });
    }

    @Scheduled(fixedRate = SIX_HOURS_MS, initialDelay = SIX_HOURS_MS)
    public void scheduledRefresh() {
        exchangeRateService.fetchFromApi();
    }
}
