package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.AuthContext;
import com.seatech.minsu.dto.BookingCreateRequest;
import com.seatech.minsu.dto.QuoteVO;
import com.seatech.minsu.entity.Booking;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 下单并发闸门：占用校验与落单之间存在双预订窗口，
 * 必须先对房间行加锁（事务内 FOR UPDATE）串行化同房源并发下单。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingControllerTest {

    @Mock
    BookingService bookingService;
    @Mock
    BookingMapper bookingMapper;
    @Mock
    RoomService roomService;
    @Mock
    MemberService memberService;
    @Mock
    PaymentService paymentService;
    @Mock
    RefundRecordService refundRecordService;
    @Mock
    RefundService refundService;
    @Mock
    PriceService priceService;
    @Mock
    PromotionMapper promotionMapper;

    BookingController controller;

    LocalDate checkin;
    LocalDate checkout;

    @BeforeEach
    void setUp() {
        controller = new BookingController(bookingService, bookingMapper, roomService,
                memberService, paymentService, refundRecordService, refundService,
                priceService, promotionMapper);
        AuthContext.set(5L);
        checkin = LocalDate.now().plusDays(1);
        checkout = LocalDate.now().plusDays(2);

        Room room = new Room();
        room.setId(10L);
        room.setRoomStatus(0);
        when(roomService.getById(10L)).thenReturn(room);

        QuoteVO quote = new QuoteVO();
        quote.setNights(1);
        quote.setUnitPrice(new BigDecimal("12500"));
        quote.setRoomFee(new BigDecimal("12500"));
        quote.setPromoDiscount(BigDecimal.ZERO);
        quote.setMemberDiscount(BigDecimal.ZERO);
        quote.setTotalAmount(new BigDecimal("12500"));
        quote.setCurrency("jpy");
        when(priceService.quote(any(), any(), any(), any(), any())).thenReturn(quote);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    private BookingCreateRequest req() {
        BookingCreateRequest r = new BookingCreateRequest();
        r.setRoomId(10L);
        r.setCheckinDate(checkin);
        r.setCheckoutDate(checkout);
        r.setGuestName("张三");
        r.setContactPhone("13800000000");
        return r;
    }

    @Test
    @DisplayName("下单先锁房间行再做占用校验（并发双预订闸门）")
    void locksRoomBeforeOccupancyCheck() {
        when(bookingService.isRoomOccupied(10L, checkin, checkout)).thenReturn(false);
        controller.create(req());

        InOrder order = inOrder(roomService, bookingService);
        order.verify(roomService).lockForBooking(10L);
        order.verify(bookingService).isRoomOccupied(10L, checkin, checkout);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingService).save(captor.capture());
        Booking saved = captor.getValue();
        assertEquals(0, saved.getPayStatus());
        assertEquals(0, saved.getBookingStatus());
        assertEquals(0, saved.getTotalAmount().compareTo(new BigDecimal("12500")));
        assertEquals("jpy", saved.getCurrency());
    }

    @Test
    @DisplayName("日期被占用时拒绝下单")
    void rejectsOccupiedDates() {
        when(bookingService.isRoomOccupied(10L, checkin, checkout)).thenReturn(true);
        assertThrows(BusinessException.class, () -> controller.create(req()));
        verify(bookingService, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("入住人与电话必填")
    void requiresGuestInfo() {
        BookingCreateRequest r = req();
        r.setGuestName(" ");
        assertThrows(BusinessException.class, () -> controller.create(r));
        verify(roomService, never()).lockForBooking(anyLong());
    }
}
