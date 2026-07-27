package com.seatech.minsu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 1登录 2新增 3修改 4删除 5其他 */
    private Integer logType;
    private Long adminId;
    private String operatorName;
    private String content;
    private String ip;
    private LocalDateTime createTime;
}
