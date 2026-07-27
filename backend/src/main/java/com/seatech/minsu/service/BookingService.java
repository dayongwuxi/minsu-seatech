package com.seatech.minsu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seatech.minsu.entity.Booking;

import java.time.LocalDate;

public interface BookingService extends IService<Booking> {

    /** 房间在 [checkinDate, checkoutDate) 区间是否已被占用(booking_status 0/1/2) */
    boolean isRoomOccupied(Long roomId, LocalDate checkinDate, LocalDate checkoutDate);
}
