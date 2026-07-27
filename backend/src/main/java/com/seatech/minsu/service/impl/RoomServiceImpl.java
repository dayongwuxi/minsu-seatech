package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.Room;
import com.seatech.minsu.mapper.RoomMapper;
import com.seatech.minsu.service.RoomService;
import org.springframework.stereotype.Service;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    @Override
    public void lockForBooking(Long roomId) {
        baseMapper.lockById(roomId);
    }
}
