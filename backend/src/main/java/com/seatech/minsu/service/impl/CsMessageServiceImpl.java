package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.CsMessage;
import com.seatech.minsu.mapper.CsMessageMapper;
import com.seatech.minsu.service.CsMessageService;
import org.springframework.stereotype.Service;

@Service
public class CsMessageServiceImpl extends ServiceImpl<CsMessageMapper, CsMessage> implements CsMessageService {
}
