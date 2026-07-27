package com.seatech.minsu.controller.admin;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.RateUpdateRequest;
import com.seatech.minsu.dto.RefreshResultVO;
import com.seatech.minsu.entity.ExchangeRate;
import com.seatech.minsu.service.ExchangeRateService;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** 汇率管理 */
@RestController
@RequestMapping("/api/admin/exchange-rates")
@RequiredArgsConstructor
public class AdminExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<List<ExchangeRate>> list() {
        return Result.ok(exchangeRateService.listAll());
    }

    /** 手工设置汇率（source=2 且锁定自动更新） */
    @PutMapping("/{code}")
    public Result<Void> manualSet(@PathVariable String code, @RequestBody RateUpdateRequest req) {
        exchangeRateService.manualSet(code, req.getRate());
        operationLogService.record(3, AuthContext.get(), "手工设置汇率: " + code + " = " + req.getRate());
        return Result.ok();
    }

    /** 恢复自动更新并立即单币种刷新 */
    @PutMapping("/{code}/auto")
    public Result<Void> enableAuto(@PathVariable String code) {
        exchangeRateService.enableAuto(code);
        operationLogService.record(3, AuthContext.get(), "恢复汇率自动更新: " + code);
        return Result.ok();
    }

    /** 全量立即刷新 */
    @PostMapping("/refresh")
    public Result<RefreshResultVO> refresh() {
        int updated = exchangeRateService.fetchFromApi();
        RefreshResultVO vo = new RefreshResultVO();
        vo.setUpdated(updated);
        vo.setFetchedAt(LocalDateTime.now());
        operationLogService.record(3, AuthContext.get(), "手动刷新汇率, 更新 " + updated + " 条");
        return Result.ok(vo);
    }
}
