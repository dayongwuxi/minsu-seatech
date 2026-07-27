package com.seatech.minsu.service;

import java.math.BigDecimal;
import java.util.Map;

/** 汇率抓取抽象（便于替换数据源与单元测试） */
public interface RateFetcher {

    /**
     * 抓取以 baseCurrency 为基准的汇率表。
     *
     * @return currencyCode → 1 base 兑换量
     */
    Map<String, BigDecimal> fetchRates(String baseCurrency) throws Exception;
}
