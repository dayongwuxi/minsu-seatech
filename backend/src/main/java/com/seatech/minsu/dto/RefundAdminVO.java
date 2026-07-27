package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理端退款申请列表项 */
@Data
public class RefundAdminVO {
    private Long id;
    private Long bookingId;
    private Long memberId;
    private BigDecimal refundAmount;
    private String reason;
    private Integer status;
    private Integer channel;
    private String stripeRefundId;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    private Long handlerId;
    private String orderNo;
    private Integer bookingStatus;
    private Integer payStatus;
    private String memberName;
    private String memberUsername;
}
