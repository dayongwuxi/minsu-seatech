package com.seatech.minsu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.seatech.minsu.entity.Admin;
import com.seatech.minsu.entity.OperationLog;
import com.seatech.minsu.mapper.AdminMapper;
import com.seatech.minsu.mapper.OperationLogMapper;
import com.seatech.minsu.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private final AdminMapper adminMapper;

    @Override
    public void record(int logType, Long adminId, String content) {
        OperationLog log = new OperationLog();
        log.setLogType(logType);
        log.setAdminId(adminId);
        if (adminId != null) {
            Admin admin = adminMapper.selectById(adminId);
            if (admin != null) {
                log.setOperatorName(admin.getName() != null ? admin.getName() : admin.getUsername());
            }
        }
        log.setContent(content);
        save(log);
    }
}
