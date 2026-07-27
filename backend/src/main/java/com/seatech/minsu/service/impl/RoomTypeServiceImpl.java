package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.RoomType;
import com.seatech.minsu.mapper.RoomTypeMapper;
import com.seatech.minsu.service.RoomTypeService;
import org.springframework.stereotype.Service;

@Service
public class RoomTypeServiceImpl extends ServiceImpl<RoomTypeMapper, RoomType> implements RoomTypeService {
}
