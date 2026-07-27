package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.RoomImage;
import com.seatech.minsu.mapper.RoomImageMapper;
import com.seatech.minsu.service.RoomImageService;
import org.springframework.stereotype.Service;

@Service
public class RoomImageServiceImpl extends ServiceImpl<RoomImageMapper, RoomImage> implements RoomImageService {
}
