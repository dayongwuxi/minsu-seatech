package com.seatech.minsu.controller;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.dto.ExchangeRatesVO;
import com.seatech.minsu.dto.RateItemVO;
import com.seatech.minsu.entity.ExchangeRate;
import com.seatech.minsu.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/** 公开汇率接口（免登录；展示换算用，创建订单与扣款一律基准币=PAYMENT_CURRENCY） */
@RestController
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/api/exchange-rates")
    public Result<ExchangeRatesVO> rates() {
        List<RateItemVO> items = exchangeRateService.listEnabled().stream()
                .map(this::toItem)
                .collect(Collectors.toList());
        ExchangeRatesVO vo = new ExchangeRatesVO();
        vo.setBase(exchangeRateService.baseCode());
        vo.setRates(items);
        return Result.ok(vo);
    }

    private RateItemVO toItem(ExchangeRate row) {
        RateItemVO item = new RateItemVO();
        item.setCode(row.getCurrencyCode());
        item.setName(row.getCurrencyName());
        item.setSymbol(row.getSymbol());
        item.setRate(row.getRate());
        item.setUpdateTime(row.getUpdateTime());
        return item;
    }
}
