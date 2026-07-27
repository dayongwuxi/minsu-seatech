package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.MemberType;
import com.seatech.minsu.mapper.MemberTypeMapper;
import com.seatech.minsu.service.MemberTypeService;
import org.springframework.stereotype.Service;

@Service
public class MemberTypeServiceImpl extends ServiceImpl<MemberTypeMapper, MemberType> implements MemberTypeService {
}
