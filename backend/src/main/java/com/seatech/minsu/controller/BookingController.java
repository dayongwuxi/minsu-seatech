package com.seatech.minsu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.BookingCreateRequest;
import com.seatech.minsu.dto.BookingDetailVO;
import com.seatech.minsu.dto.BookingUserVO;
import com.seatech.minsu.dto.MemberVO;
import com.seatech.minsu.dto.QuoteRequest;
import com.seatech.minsu.dto.QuoteVO;
import com.seatech.minsu.dto.RefundApplyRequest;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.mapper.BookingMapper;
import com.seatech.minsu.mapper.PromotionMapper;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.PaymentService;
import com.seatech.minsu.service.PriceService;
import com.seatech.minsu.service.RefundRecordService;
import com.seatech.minsu.service.RefundService;
import com.seatech.minsu.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 会员端预约报价/下单/列表/取消/退款申请 */
@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final RoomService roomService;
    private final MemberService memberService;
    private final PaymentService paymentService;
    private final RefundRecordService refundRecordService;
    private final RefundService refundService;
    private final PriceService priceService;
    private final PromotionMapper promotionMapper;

    /** 实时报价：Airbnb 式价格明细（房费小计/促销/会员折扣/合计） */
    @PostMapping("/api/bookings/quote")
    public Result<QuoteVO> quote(@RequestBody QuoteRequest req) {
        return Result.ok(priceService.quote(AuthContext.get(), req.getRoomId(),
                req.getCheckinDate(), req.getCheckoutDate(), req.getPromoCode()));
    }

    /** 下单：服务端以 PriceService 重算价格明细并落库，防篡改 */
    @PostMapping("/api/bookings")
    @Transactional(rollbackFor = Exception.class)
    public Result<Booking> create(@RequestBody BookingCreateRequest req) {
        Long memberId = AuthContext.get();
        LocalDate checkin = req.getCheckinDate();
        LocalDate checkout = req.getCheckoutDate();
        if (!StringUtils.hasText(req.getGuestName()) || !StringUtils.hasText(req.getContactPhone())) {
            throw new BusinessException("入住人姓名与联系电话不能为空");
        }
        // 价格与日期/房间有效性校验（内部含日期合法性、房间上架校验）
        QuoteVO quote = priceService.quote(memberId, req.getRoomId(), checkin, checkout, req.getPromoCode());

        Room room = roomService.getById(req.getRoomId());
        if (room.getRoomStatus() != null && room.getRoomStatus() == 1) {
            throw new BusinessException("房间维修中，暂不可预订");
        }
        // 并发闸门：事务内锁房间行，占用校验与落单之间不再有双预订窗口
        roomService.lockForBooking(room.getId());
        if (bookingService.isRoomOccupied(room.getId(), checkin, checkout)) {
            throw new BusinessException("该房间在所选日期已被预订");
        }

        Booking booking = new Booking();
        booking.setOrderNo(NoGenerator.gen("DD"));
        booking.setMemberId(memberId);
        booking.setRoomId(room.getId());
        booking.setCheckinDate(checkin);
        booking.setCheckoutDate(checkout);
        booking.setNights(quote.getNights());
        booking.setGuestCount(req.getGuestCount() == null ? 1 : req.getGuestCount());
        booking.setGuestName(req.getGuestName());
        booking.setContactPhone(req.getContactPhone());
        booking.setUnitPrice(quote.getUnitPrice());
        booking.setRoomFee(quote.getRoomFee());
        booking.setPromoDiscount(quote.getPromoDiscount());
        booking.setMemberDiscount(quote.getMemberDiscount());
        booking.setPromotionId(quote.getPromotionId());
        booking.setPromoCode(quote.getPromoCode());
        booking.setDiscountAmount(quote.getPromoDiscount().add(quote.getMemberDiscount()));
        booking.setTotalAmount(quote.getTotalAmount());
        booking.setCurrency(quote.getCurrency());
        booking.setPayStatus(0);
        booking.setBookingStatus(0);
        booking.setUserRemark(req.getUserRemark());

        // 命中促销：并发安全占用名额，抢完则失败回滚
        if (quote.getPromotionId() != null && promotionMapper.consumeUsage(quote.getPromotionId()) == 0) {
            throw new BusinessException("该促销名额已被抢完，请重新报价下单");
        }
        bookingService.save(booking);
        return Result.ok(booking);
    }

    /** 我的预约分页, status tab: waitPay/paid/cancelled/finished，不传查全部 */
    @GetMapping("/api/user/bookings")
    public Result<PageResult<BookingUserVO>> myBookings(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String status) {
        IPage<BookingUserVO> page = bookingMapper.selectUserPage(
                new Page<>(current, size), AuthContext.get(), status);
        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/api/bookings/{orderNo}")
    public Result<BookingDetailVO> detail(@PathVariable String orderNo) {
        Booking booking = getOwnedBooking(orderNo);
        BookingDetailVO vo = new BookingDetailVO();
        vo.setBooking(booking);
        Member member = memberService.getById(booking.getMemberId());
        if (member != null) {
            vo.setMember(MemberVO.of(member, null, true));
        }
        vo.setRoom(roomService.getById(booking.getRoomId()));
        vo.setPayment(paymentService.lambdaQuery()
                .eq(Payment::getBookingId, booking.getId())
                .orderByDesc(Payment::getId)
                .list().stream().findFirst().orElse(null));
        vo.setRefund(refundRecordService.lambdaQuery()
                .eq(RefundRecord::getBookingId, booking.getId())
                .orderByDesc(RefundRecord::getId)
                .list().stream().findFirst().orElse(null));
        return Result.ok(vo);
    }

    /** 取消订单：待支付直接取消；已支付未入住取消并生成退款申请 */
    @PostMapping("/api/bookings/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo,
                               @RequestBody(required = false) RefundApplyRequest req) {
        Booking booking = getOwnedBooking(orderNo);
        int bs = booking.getBookingStatus();
        int ps = booking.getPayStatus();
        if (bs != 0 && bs != 1) {
            throw new BusinessException("当前订单状态不可取消");
        }
        booking.setBookingStatus(4);
        bookingService.updateById(booking);
        if (ps == 1) {
            refundService.createApply(booking,
                    req == null || !StringUtils.hasText(req.getReason()) ? "用户取消订单" : req.getReason());
        }
        return Result.ok();
    }

    /** 已完成订单申请退款 */
    @PostMapping("/api/bookings/{orderNo}/refund")
    public Result<Void> applyRefund(@PathVariable String orderNo,
                                    @RequestBody(required = false) RefundApplyRequest req) {
        Booking booking = getOwnedBooking(orderNo);
        if (booking.getBookingStatus() != 3 || booking.getPayStatus() != 1) {
            throw new BusinessException("仅已完成且已支付的订单可申请退款");
        }
        long pending = refundRecordService.lambdaQuery()
                .eq(RefundRecord::getBookingId, booking.getId())
                .in(RefundRecord::getStatus, 0, 1)
                .count();
        if (pending > 0) {
            throw new BusinessException("已有退款申请正在处理中");
        }
        refundService.createApply(booking, req == null ? null : req.getReason());
        return Result.ok();
    }

    private Booking getOwnedBooking(String orderNo) {
        Booking booking = bookingService.lambdaQuery()
                .eq(Booking::getOrderNo, orderNo)
                .one();
        if (booking == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!booking.getMemberId().equals(AuthContext.get())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该订单");
        }
        return booking;
    }
}
