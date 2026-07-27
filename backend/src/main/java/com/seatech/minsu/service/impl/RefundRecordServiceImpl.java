package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.RefundRecord;
import com.seatech.minsu.mapper.RefundRecordMapper;
import com.seatech.minsu.service.RefundRecordService;
import org.springframework.stereotype.Service;

@Service
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements RefundRecordService {
}
