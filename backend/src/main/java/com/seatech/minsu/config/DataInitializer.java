package com.seatech.minsu.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.seatech.minsu.entity.Admin;
import com.seatech.minsu.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 首次启动时创建初始管理员 admin/admin123（避免在 SQL 中硬编码 BCrypt 哈希） */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminMapper.selectCount(new QueryWrapper<>()) > 0) {
            return;
        }
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setName("管理员");
        admin.setRoleId(1L);
        admin.setStatus(1);
        adminMapper.insert(admin);
        log.info("已创建初始管理员账号 admin / admin123，请首次登录后立即修改密码");
    }
}
