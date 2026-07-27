package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.Menu;
import com.seatech.minsu.mapper.MenuMapper;
import com.seatech.minsu.service.MenuService;
import org.springframework.stereotype.Service;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {
}
