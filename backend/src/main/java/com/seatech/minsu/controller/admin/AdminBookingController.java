package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.NoGenerator;
import com.seatech.minsu.common.PageResult;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.BookingAdminVO;
import com.seatech.minsu.dto.BookingDetailVO;
import com.seatech.minsu.dto.BookingQuery;
import com.seatech.minsu.dto.MemberVO;
import com.seatech.minsu.dto.RemarkRequest;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.entity.CheckinRecord;
import com.seatech.minsu.entity.Member;
import com.seatech.minsu.entity.Payment;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.mapper.BookingMapper;
import com.seatech.minsu.service.BookingService;
import com.seatech.minsu.service.CheckinRecordService;
import com.seatech.minsu.service.MemberService;
import com.seatech.minsu.service.OperationLogService;
import com.seatech.minsu.service.PaymentService;
import com.seatech.minsu.service.RefundRecordService;
import com.seatech.minsu.service.RefundService;
import com.seatech.minsu.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 预定记录管理 */
@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final MemberService memberService;
    private final RoomService roomService;
    private final PaymentService paymentService;
    private final RefundRecordService refundRecordService;
    private final RefundService refundService;
    private final CheckinRecordService checkinRecordService;
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<BookingAdminVO>> page(BookingQuery query) {
        IPage<BookingAdminVO> page = bookingMapper.selectAdminPage(
                new Page<>(query.getCurrent(), query.getSize()), query);
        return Result.ok(PageResult.of(page));
    }

    /** 聚合详情：订单+会员+房间+支付+退款 */
    @GetMapping("/{id}")
    public Result<BookingDetailVO> detail(@PathVariable Long id) {
        Booking booking = requireBooking(id);
        BookingDetailVO vo = new BookingDetailVO();
        vo.setBooking(booking);
        Member member = memberService.getById(booking.getMemberId());
        if (member != null) {
            vo.setMember(MemberVO.of(member, null, false));
        }
        vo.setRoom(roomService.getById(booking.getRoomId()));
        vo.setPayment(paymentService.lambdaQuery()
                .eq(Payment::getBookingId, id)
                .orderByDesc(Payment::getId)
                .list().stream().findFirst().orElse(null));
        vo.setRefund(refundRecordService.lambdaQuery()
                .eq(RefundRecord::getBookingId, id)
                .orderByDesc(RefundRecord::getId)
                .list().stream().findFirst().orElse(null));
        return Result.ok(vo);
    }

    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        Booking booking = requireBooking(id);
        if (booking.getBookingStatus() != 0) {
            throw new BusinessException("仅待确认订单可确认");
        }
        booking.setBookingStatus(1);
        bookingService.updateById(booking);
        operationLogService.record(3, AuthContext.get(), "确认订单: " + booking.getOrderNo());
        return Result.ok();
    }

    /** 取消订单：已支付单取消需联动退款申请（与用户端取消口径一致），交退款管理闭环处理 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Booking booking = requireBooking(id);
        if (booking.getBookingStatus() != 0 && booking.getBookingStatus() != 1) {
            throw new BusinessException("当前订单状态不可取消");
        }
        booking.setBookingStatus(4);
        bookingService.updateById(booking);
        if (booking.getPayStatus() != null && booking.getPayStatus() == 1) {
            long pending = refundRecordService.lambdaQuery()
                    .eq(RefundRecord::getBookingId, booking.getId())
                    .in(RefundRecord::getStatus, 0, 1)
                    .count();
            if (pending == 0) {
                refundService.createApply(booking, "管理员取消订单");
            }
        }
        operationLogService.record(3, AuthContext.get(), "取消订单: " + booking.getOrderNo());
        return Result.ok();
    }

    /** 办理入住：生成入住记录并将订单置为已入住 */
    @PostMapping("/{id}/checkin")
    public Result<Void> checkin(@PathVariable Long id) {
        Booking booking = requireBooking(id);
        if (booking.getPayStatus() != 1) {
            throw new BusinessException("订单未支付，不能办理入住");
        }
        if (booking.getBookingStatus() != 0 && booking.getBookingStatus() != 1) {
            throw new BusinessException("当前订单状态不可办理入住");
        }
        CheckinRecord record = new CheckinRecord();
        record.setCheckinNo(NoGenerator.gen("CR"));
        record.setBookingId(booking.getId());
        record.setMemberId(booking.getMemberId());
        record.setRoomId(booking.getRoomId());
        record.setGuestName(booking.getGuestName());
        record.setCheckinTime(LocalDateTime.now());
        record.setStatus(1);
        record.setIsReviewed(0);
        checkinRecordService.save(record);

        booking.setBookingStatus(2);
        bookingService.updateById(booking);
        operationLogService.record(2, AuthContext.get(), "办理入住: " + booking.getOrderNo());
        return Result.ok();
    }

    /** 同意退款：统一走 RefundService（Stripe 单退款中等待 webhook，Mock 单即时退款） */
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        Booking booking = requireBooking(id);
        refundService.adminDirectRefund(booking, AuthContext.get());
        operationLogService.record(3, AuthContext.get(), "同意退款: " + booking.getOrderNo());
        return Result.ok();
    }

    @PutMapping("/{id}/remark")
    public Result<Void> remark(@PathVariable Long id, @RequestBody RemarkRequest req) {
        Booking booking = requireBooking(id);
        booking.setAdminRemark(req.getRemark());
        bookingService.updateById(booking);
        operationLogService.record(3, AuthContext.get(), "修改订单备注: " + booking.getOrderNo());
        return Result.ok();
    }

    private Booking requireBooking(Long id) {
        Booking booking = bookingService.getById(id);
        if (booking == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return booking;
    }
}
