package com.seatech.minsu.dto;

import lombok.Data;

/** 通用状态修改请求体 { status } */
@Data
public class StatusRequest {
    private Integer status;
}
