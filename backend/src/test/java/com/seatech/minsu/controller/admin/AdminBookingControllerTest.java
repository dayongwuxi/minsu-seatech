package com.seatech.minsu.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.entity.Booking;
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
import com.seatech.minsu.testutil.ChainMocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端取消订单：已支付单取消必须联动退款申请（否则钱已收、房已放、无退款闭环），
 * 已有进行中退款单时不重复生成。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBookingControllerTest {

    @Mock
    BookingService bookingService;
    @Mock
    BookingMapper bookingMapper;
    @Mock
    MemberService memberService;
    @Mock
    RoomService roomService;
    @Mock
    PaymentService paymentService;
    @Mock
    RefundRecordService refundRecordService;
    @Mock
    RefundService refundService;
    @Mock
    CheckinRecordService checkinRecordService;
    @Mock
    OperationLogService operationLogService;

    LambdaQueryChainWrapper<RefundRecord> refundQuery;

    AdminBookingController controller;

    Booking booking;

    @BeforeEach
    void setUp() {
        refundQuery = ChainMocks.queryChain();
        when(refundRecordService.lambdaQuery()).thenReturn(refundQuery);
        controller = new AdminBookingController(bookingService, bookingMapper, memberService,
                roomService, paymentService, refundRecordService, refundService,
                checkinRecordService, operationLogService);
        booking = new Booking();
        booking.setId(9L);
        booking.setOrderNo("DD1");
        booking.setBookingStatus(1);
        booking.setPayStatus(0);
        when(bookingService.getById(9L)).thenReturn(booking);
        AuthContext.set(88L);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("未支付订单取消：直接置已取消，不生成退款申请")
    void cancelUnpaidBooking() {
        controller.cancel(9L);
        assertEquals(4, booking.getBookingStatus());
        verify(refundService, never()).createApply(any(), anyString());
    }

    @Test
    @DisplayName("已支付订单取消：置已取消并生成退款申请供退款管理处理")
    void cancelPaidBookingCreatesRefundApply() {
        booking.setPayStatus(1);
        when(refundQuery.count()).thenReturn(0L);
        controller.cancel(9L);
        assertEquals(4, booking.getBookingStatus());
        verify(refundService).createApply(eq(booking), anyString());
    }

    @Test
    @DisplayName("已支付且已有进行中退款单：取消时不重复生成申请")
    void cancelPaidBookingSkipsDuplicateApply() {
        booking.setPayStatus(1);
        when(refundQuery.count()).thenReturn(1L);
        controller.cancel(9L);
        assertEquals(4, booking.getBookingStatus());
        verify(refundService, never()).createApply(any(), anyString());
    }

    @Test
    @DisplayName("已入住/已完成订单不可取消")
    void cancelRejectedForCheckedIn() {
        booking.setBookingStatus(2);
        assertThrows(BusinessException.class, () -> controller.cancel(9L));
    }
}
