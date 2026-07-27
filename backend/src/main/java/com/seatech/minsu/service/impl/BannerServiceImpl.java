package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.Banner;
import com.seatech.minsu.mapper.BannerMapper;
import com.seatech.minsu.service.BannerService;
import org.springframework.stereotype.Service;

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
}
