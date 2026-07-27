package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员端我的评价项 */
@Data
public class MyReviewVO {
    private Long id;
    private String reviewNo;
    private Long roomId;
    private Long bookingId;
    private BigDecimal rating;
    private String content;
    private Integer auditStatus;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private String roomName;
    private String roomCover;
}
