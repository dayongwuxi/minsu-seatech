package com.seatech.minsu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seatech.minsu.entity.Room;

public interface RoomService extends IService<Room> {

    /** 下单前房间行级锁（须在事务内调用），串行化同房源并发下单 */
    void lockForBooking(Long roomId);
}
