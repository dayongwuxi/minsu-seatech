package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.Promotion;
import com.seatech.minsu.mapper.PromotionMapper;
import com.seatech.minsu.service.PromotionService;
import org.springframework.stereotype.Service;

@Service
public class PromotionServiceImpl extends ServiceImpl<PromotionMapper, Promotion> implements PromotionService {
}
