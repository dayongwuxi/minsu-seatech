package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 管理端评价列表项 */
@Data
public class ReviewAdminVO {
    private Long id;
    private String reviewNo;
    private Long memberId;
    private Long roomId;
    private Long bookingId;
    private BigDecimal rating;
    private String content;
    private Integer auditStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private String roomName;
    private String memberName;
    private String memberUsername;
}
