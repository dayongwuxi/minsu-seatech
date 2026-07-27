package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.Booking;
import com.seatech.minsu.mapper.BookingMapper;
import com.seatech.minsu.service.BookingService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    @Override
    public boolean isRoomOccupied(Long roomId, LocalDate checkinDate, LocalDate checkoutDate) {
        return lambdaQuery()
                .eq(Booking::getRoomId, roomId)
                .in(Booking::getBookingStatus, 0, 1, 2)
                .lt(Booking::getCheckinDate, checkoutDate)
                .gt(Booking::getCheckoutDate, checkinDate)
                .count() > 0;
    }
}
