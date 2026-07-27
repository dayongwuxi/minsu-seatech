package com.seatech.minsu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.seatech.minsu.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    /**
     * 记录后台操作日志
     *
     * @param logType 1登录 2新增 3修改 4删除 5其他
     * @param adminId 操作管理员id
     * @param content 操作内容
     */
    void record(int logType, Long adminId, String content);
}
