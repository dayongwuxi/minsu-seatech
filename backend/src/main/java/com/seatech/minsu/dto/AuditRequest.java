package com.seatech.minsu.dto;

import lombok.Data;

/** 评价审核请求体 { auditStatus } 1通过 2不通过 */
@Data
public class AuditRequest {
    private Integer auditStatus;
}
