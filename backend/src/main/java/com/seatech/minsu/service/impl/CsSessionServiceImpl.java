package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.CsSession;
import com.seatech.minsu.mapper.CsSessionMapper;
import com.seatech.minsu.service.CsSessionService;
import org.springframework.stereotype.Service;

@Service
public class CsSessionServiceImpl extends ServiceImpl<CsSessionMapper, CsSession> implements CsSessionService {
}
