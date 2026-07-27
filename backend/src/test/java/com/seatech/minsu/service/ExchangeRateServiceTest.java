package com.seatech.minsu.service;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.entity.ExchangeRate;
import com.seatech.minsu.mapper.ExchangeRateMapper;
import com.seatech.minsu.service.impl.ErApiRateFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ExchangeRateService 单元测试（mock mapper + fetcher） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateMapper mapper;
    @Mock
    private RateFetcher fetcher;

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        // 基准货币跟随站点结算货币（本测试固定 cny，保持既有断言不变）
        service = new ExchangeRateService(mapper, fetcher, "cny");
        when(mapper.updateById(any(ExchangeRate.class))).thenReturn(1);
    }

    @Test
    @DisplayName("基准货币跟随 PAYMENT_CURRENCY：jpy 站点以 JPY 为基准抓取")
    void baseCurrencyFollowsSiteCurrency() throws Exception {
        ExchangeRateService jpyService = new ExchangeRateService(mapper, fetcher, "jpy");
        assertEquals("JPY", jpyService.baseCode());
        when(mapper.selectList(any())).thenReturn(List.of(row("CNY", "0.04", 1, 1)));
        when(fetcher.fetchRates("JPY")).thenReturn(Map.of("CNY", new BigDecimal("0.0414")));
        jpyService.fetchFromApi();
        verify(fetcher).fetchRates("JPY");
    }

    private ExchangeRate row(String code, String rate, int autoUpdate, int source) {
        ExchangeRate r = new ExchangeRate();
        r.setId((long) code.hashCode());
        r.setCurrencyCode(code);
        r.setCurrencyName(code);
        r.setSymbol("$");
        r.setRate(new BigDecimal(rate));
        r.setAutoUpdate(autoUpdate);
        r.setSource(source);
        r.setStatus(1);
        return r;
    }

    @Test
    @DisplayName("API 刷新: 仅更新 auto_update=1 的行, 手工锁定行不被覆盖")
    void refreshSkipsManuallyLockedRows() throws Exception {
        when(fetcher.fetchRates("CNY")).thenReturn(Map.of(
                "USD", new BigDecimal("0.1394"),
                "JPY", new BigDecimal("21.30")));
        ExchangeRate usd = row("USD", "0.14", 1, 1);
        ExchangeRate jpyLocked = row("JPY", "20.00", 0, 2); // 管理员手工锁定
        when(mapper.selectList(any())).thenReturn(List.of(usd, jpyLocked));

        int updated = service.fetchFromApi();

        assertEquals(1, updated);
        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(mapper, times(1)).updateById(captor.capture());
        ExchangeRate saved = captor.getValue();
        assertEquals("USD", saved.getCurrencyCode());
        assertEquals(0, saved.getRate().compareTo(new BigDecimal("0.1394")));
        assertEquals(1, saved.getSource().intValue());
        // 锁定行保持原值
        assertEquals(0, jpyLocked.getRate().compareTo(new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("API 刷新: 更新行必须刷新 updateTime（否则前端看不出变化）")
    void refreshStampsUpdateTime() throws Exception {
        when(fetcher.fetchRates("CNY")).thenReturn(Map.of("USD", new BigDecimal("0.1394")));
        ExchangeRate usd = row("USD", "0.14", 1, 1);
        usd.setUpdateTime(LocalDateTime.of(2026, 7, 8, 0, 0)); // 旧时间戳
        when(mapper.selectList(any())).thenReturn(List.of(usd));

        LocalDateTime before = LocalDateTime.now();
        service.fetchFromApi();

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(mapper).updateById(captor.capture());
        LocalDateTime saved = captor.getValue().getUpdateTime();
        assertNotNull(saved);
        assertTrue(!saved.isBefore(before), "updateTime 应被刷新为当前时间");
    }

    @Test
    @DisplayName("API 无对应币种时跳过该行")
    void refreshSkipsMissingCurrency() throws Exception {
        when(fetcher.fetchRates("CNY")).thenReturn(Map.of("USD", new BigDecimal("0.1394")));
        ExchangeRate eur = row("EUR", "0.12", 1, 1);
        when(mapper.selectList(any())).thenReturn(List.of(eur));

        assertEquals(0, service.fetchFromApi());
        verify(mapper, never()).updateById(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("API 异常: 记日志返回0, 不抛出且保留旧值")
    void refreshApiFailureReturnsZero() throws Exception {
        when(fetcher.fetchRates(anyString())).thenThrow(new IOException("timeout"));
        assertEquals(0, service.fetchFromApi());
        verify(mapper, never()).selectList(any());
        verify(mapper, never()).updateById(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("手工设置汇率: source=2 且 auto_update=0 锁定")
    void manualSetLocksRow() {
        ExchangeRate usd = row("USD", "0.14", 1, 1);
        when(mapper.selectOne(any())).thenReturn(usd);

        service.manualSet("usd", new BigDecimal("0.15"));

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getRate().compareTo(new BigDecimal("0.15")));
        assertEquals(2, captor.getValue().getSource().intValue());
        assertEquals(0, captor.getValue().getAutoUpdate().intValue());
    }

    @Test
    @DisplayName("手工汇率必须大于0")
    void manualSetRejectsNonPositive() {
        assertThrows(BusinessException.class, () -> service.manualSet("USD", BigDecimal.ZERO));
        assertThrows(BusinessException.class, () -> service.manualSet("USD", new BigDecimal("-1")));
        assertThrows(BusinessException.class, () -> service.manualSet("USD", null));
    }

    @Test
    @DisplayName("恢复自动更新: auto_update=1 并立即单币种刷新")
    void enableAutoRefreshesImmediately() throws Exception {
        ExchangeRate jpy = row("JPY", "20.00", 0, 2);
        when(mapper.selectOne(any())).thenReturn(jpy);
        when(fetcher.fetchRates("CNY")).thenReturn(Map.of("JPY", new BigDecimal("21.55")));

        service.enableAuto("JPY");

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getAutoUpdate().intValue());
        assertEquals(0, captor.getValue().getRate().compareTo(new BigDecimal("21.55")));
        assertEquals(1, captor.getValue().getSource().intValue());
    }

    @Test
    @DisplayName("恢复自动更新时抓取失败: 仅恢复标记保留旧汇率")
    void enableAutoKeepsOldRateOnFetchFailure() throws Exception {
        ExchangeRate jpy = row("JPY", "20.00", 0, 2);
        when(mapper.selectOne(any())).thenReturn(jpy);
        when(fetcher.fetchRates(anyString())).thenThrow(new IOException("timeout"));

        service.enableAuto("JPY");

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getAutoUpdate().intValue());
        assertEquals(0, captor.getValue().getRate().compareTo(new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("er-api 响应解析: result=success 提取 rates")
    void erApiParseSuccess() throws Exception {
        String json = """
                {"result":"success","base_code":"CNY",
                 "rates":{"CNY":1,"USD":0.1394,"JPY":21.33,"EUR":0.1287}}
                """;
        Map<String, BigDecimal> rates = new ErApiRateFetcher().parse(json);
        assertEquals(4, rates.size());
        assertEquals(0, rates.get("USD").compareTo(new BigDecimal("0.1394")));
        assertEquals(0, rates.get("JPY").compareTo(new BigDecimal("21.33")));
    }

    @Test
    @DisplayName("er-api 响应解析: result!=success 抛异常")
    void erApiParseFailure() {
        String json = "{\"result\":\"error\",\"error-type\":\"invalid-key\"}";
        Exception e = assertThrows(Exception.class, () -> new ErApiRateFetcher().parse(json));
        assertTrue(e.getMessage().contains("汇率 API"));
    }
}
