package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.dto.BookingAdminVO;
import com.seatech.minsu.dto.DashboardStatsVO;
import com.seatech.minsu.dto.HotRoomVO;
import com.seatech.minsu.dto.TrendPointVO;
import com.seatech.minsu.entity.Feedback;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.mapper.BookingMapper;
import com.seatech.minsu.mapper.CheckinRecordMapper;
import com.seatech.minsu.mapper.PaymentMapper;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.FeedbackService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 管理端数据看板 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BookingMapper bookingMapper;
    private final PaymentMapper paymentMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final BookingService bookingService;
    private final RoomService roomService;
    private final MemberService memberService;
    private final FeedbackService feedbackService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        DashboardStatsVO vo = new DashboardStatsVO();
        long todayBookings = bookingMapper.countCreatedBetween(todayStart, tomorrowStart);
        long yesterdayBookings = bookingMapper.countCreatedBetween(yesterdayStart, todayStart);
        vo.setTodayBookings(todayBookings);
        vo.setBookingsDiff(todayBookings - yesterdayBookings);

        long todayCheckins = checkinRecordMapper.countCheckinBetween(todayStart, tomorrowStart);
        long yesterdayCheckins = checkinRecordMapper.countCheckinBetween(yesterdayStart, todayStart);
        vo.setTodayCheckins(todayCheckins);
        vo.setCheckinsDiff(todayCheckins - yesterdayCheckins);

        BigDecimal todayIncome = paymentMapper.sumIncomeBetween(todayStart, tomorrowStart);
        BigDecimal yesterdayIncome = paymentMapper.sumIncomeBetween(yesterdayStart, todayStart);
        vo.setTodayIncome(todayIncome);
        vo.setIncomeDiff(todayIncome.subtract(yesterdayIncome));

        vo.setPendingFeedbacks(feedbackService.lambdaQuery().eq(Feedback::getStatus, 0).count());
        vo.setRoomTotal(roomService.count());
        vo.setAvailableRooms(roomService.lambdaQuery()
                .eq(Room::getRoomStatus, 0)
                .eq(Room::getShelfStatus, 1)
                .count());
        vo.setMemberTotal(memberService.count());
        vo.setBookingTotal(bookingService.count(new LambdaQueryWrapper<>()));
        return Result.ok(vo);
    }

    /** range: 7d / 30d / 12m */
    @GetMapping("/income-trend")
    public Result<List<TrendPointVO>> incomeTrend(@RequestParam(defaultValue = "7d") String range) {
        return Result.ok(paymentMapper.selectIncomeTrend(trendStart(range), trendFormat(range)));
    }

    @GetMapping("/booking-trend")
    public Result<List<TrendPointVO>> bookingTrend(@RequestParam(defaultValue = "7d") String range) {
        return Result.ok(bookingMapper.selectBookingTrend(trendStart(range), trendFormat(range)));
    }

    @GetMapping("/hot-rooms")
    public Result<List<HotRoomVO>> hotRooms(@RequestParam(defaultValue = "7") int days,
                                            @RequestParam(defaultValue = "3") int limit) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        return Result.ok(bookingMapper.selectHotRooms(start, limit));
    }

    @GetMapping("/latest-bookings")
    public Result<List<BookingAdminVO>> latestBookings(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(bookingMapper.selectLatest(limit));
    }

    static LocalDateTime trendStart(String range) {
        LocalDate today = LocalDate.now();
        if ("12m".equals(range)) {
            return today.minusMonths(11).withDayOfMonth(1).atStartOfDay();
        }
        if ("30d".equals(range)) {
            return today.minusDays(29).atStartOfDay();
        }
        return today.minusDays(6).atStartOfDay();
    }

    static String trendFormat(String range) {
        return "12m".equals(range) ? "%Y-%m" : "%Y-%m-%d";
    }
}
