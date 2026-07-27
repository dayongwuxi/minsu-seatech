package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.dto.IncomeQuery;
import com.seatech.minsu.dto.IncomeStatsVO;
import com.seatech.minsu.dto.IncomeVO;
import com.seatech.minsu.dto.PayMethodRatioVO;
import com.seatech.minsu.dto.TrendPointVO;
import com.seatech.minsu.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 收入管理(基于支付流水) */
@RestController
@RequestMapping("/api/admin/incomes")
@RequiredArgsConstructor
public class AdminIncomeController {

    private final PaymentMapper paymentMapper;

    @GetMapping
    public Result<PageResult<IncomeVO>> page(IncomeQuery query) {
        IPage<IncomeVO> page = paymentMapper.selectIncomePage(
                new Page<>(query.getCurrent(), query.getSize()), query);
        return Result.ok(PageResult.of(page));
    }

    /** 今日/本月/累计/退款/实际收入 + 环比增减额 */
    @GetMapping("/stats")
    public Result<IncomeStatsVO> stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();

        IncomeStatsVO vo = new IncomeStatsVO();
        BigDecimal todayIncome = paymentMapper.sumIncomeBetween(todayStart, tomorrowStart);
        BigDecimal yesterdayIncome = paymentMapper.sumIncomeBetween(yesterdayStart, todayStart);
        vo.setTodayIncome(todayIncome);
        vo.setTodayDiff(todayIncome.subtract(yesterdayIncome));

        BigDecimal monthIncome = paymentMapper.sumIncomeBetween(monthStart, tomorrowStart);
        BigDecimal lastMonthIncome = paymentMapper.sumIncomeBetween(lastMonthStart, monthStart);
        vo.setMonthIncome(monthIncome);
        vo.setMonthDiff(monthIncome.subtract(lastMonthIncome));

        BigDecimal total = paymentMapper.sumTotalIncome();
        BigDecimal refund = paymentMapper.sumRefund();
        vo.setTotalIncome(total);
        vo.setRefundTotal(refund);
        vo.setActualIncome(total.subtract(refund));
        return Result.ok(vo);
    }

    /** range: 7d / 30d / 12m */
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7d") String range) {
        return Result.ok(paymentMapper.selectIncomeTrend(
                DashboardController.trendStart(range), DashboardController.trendFormat(range)));
    }

    @GetMapping("/pay-method-ratio")
    public Result<List<PayMethodRatioVO>> payMethodRatio() {
        return Result.ok(paymentMapper.selectPayMethodRatio());
    }
}
