package com.seatech.minsu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.entity.ExchangeRate;
import com.seatech.minsu.mapper.ExchangeRateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 展示汇率管理（基准 = 站点结算货币 PAYMENT_CURRENCY，报价/扣款一律基准币）。
 * API 自动刷新仅覆盖 auto_update=1 且 status=1 的行；管理员手工改价即锁定。
 */
@Slf4j
@Service
public class ExchangeRateService {

    private final ExchangeRateMapper exchangeRateMapper;
    private final RateFetcher rateFetcher;
    private final String baseCode;

    public ExchangeRateService(ExchangeRateMapper exchangeRateMapper,
                               RateFetcher rateFetcher,
                               @Value("${PAYMENT_CURRENCY:${STRIPE_CURRENCY:cny}}") String siteCurrency) {
        this.exchangeRateMapper = exchangeRateMapper;
        this.rateFetcher = rateFetcher;
        this.baseCode = StringUtils.hasText(siteCurrency) ? siteCurrency.trim().toUpperCase() : "CNY";
    }

    /** 展示换算基准货币代码（= 站点结算货币，如 JPY） */
    public String baseCode() {
        return baseCode;
    }

    /**
     * 从汇率 API 全量刷新。任何异常记日志不抛出（保留旧值）。
     *
     * @return 实际更新条数
     */
    public int fetchFromApi() {
        Map<String, BigDecimal> rates;
        try {
            rates = rateFetcher.fetchRates(baseCode);
        } catch (Exception e) {
            log.warn("汇率 API 抓取失败, 保留旧值: {}", e.getMessage());
            return 0;
        }
        if (rates == null || rates.isEmpty()) {
            log.warn("汇率 API 返回空数据, 保留旧值");
            return 0;
        }
        int updated = 0;
        try {
            List<ExchangeRate> rows = exchangeRateMapper.selectList(
                    new LambdaQueryWrapper<ExchangeRate>().eq(ExchangeRate::getStatus, 1));
            for (ExchangeRate row : rows) {
                // 手工锁定行(auto_update=0)不被自动刷新覆盖
                if (row.getAutoUpdate() == null || row.getAutoUpdate() != 1) {
                    continue;
                }
                BigDecimal rate = rates.get(row.getCurrencyCode());
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                row.setRate(rate);
                row.setSource(1);
                row.setUpdateTime(LocalDateTime.now());
                exchangeRateMapper.updateById(row);
                updated++;
            }
        } catch (Exception e) {
            log.warn("汇率落库失败", e);
        }
        log.info("汇率刷新完成, 更新 {} 条", updated);
        return updated;
    }

    /** 启用中的汇率（公开接口用） */
    public List<ExchangeRate> listEnabled() {
        return exchangeRateMapper.selectList(new LambdaQueryWrapper<ExchangeRate>()
                .eq(ExchangeRate::getStatus, 1)
                .orderByAsc(ExchangeRate::getId));
    }

    /** 全部行（管理端用，含 autoUpdate/source） */
    public List<ExchangeRate> listAll() {
        return exchangeRateMapper.selectList(new LambdaQueryWrapper<ExchangeRate>()
                .orderByAsc(ExchangeRate::getId));
    }

    /** 管理员手工设置汇率：source=2 且 auto_update=0（锁定不被自动覆盖） */
    public void manualSet(String code, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("汇率必须大于0");
        }
        ExchangeRate row = requireByCode(code);
        row.setRate(rate);
        row.setSource(2);
        row.setAutoUpdate(0);
        row.setUpdateTime(LocalDateTime.now());
        exchangeRateMapper.updateById(row);
    }

    /** 恢复自动更新并立即单币种刷新（抓取失败则仅恢复标记） */
    public void enableAuto(String code) {
        ExchangeRate row = requireByCode(code);
        row.setAutoUpdate(1);
        try {
            Map<String, BigDecimal> rates = rateFetcher.fetchRates(baseCode);
            BigDecimal rate = rates == null ? null : rates.get(row.getCurrencyCode());
            if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                row.setRate(rate);
                row.setSource(1);
            }
        } catch (Exception e) {
            log.warn("单币种汇率刷新失败: {} - {}", code, e.getMessage());
        }
        row.setUpdateTime(LocalDateTime.now());
        exchangeRateMapper.updateById(row);
    }

    private ExchangeRate requireByCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("币种不能为空");
        }
        ExchangeRate row = exchangeRateMapper.selectOne(new LambdaQueryWrapper<ExchangeRate>()
                .eq(ExchangeRate::getCurrencyCode, code.trim().toUpperCase()));
        if (row == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "币种不存在");
        }
        return row;
    }
}
