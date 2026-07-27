package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 汇率手动刷新结果 */
@Data
public class RefreshResultVO {
    private int updated;
    private LocalDateTime fetchedAt;
}
