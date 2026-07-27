package com.seatech.minsu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 房间详情页评价项 */
@Data
public class RoomReviewVO {
    private Long id;
    private BigDecimal rating;
    private String content;
    private LocalDateTime createTime;
    private String replyContent;
    private LocalDateTime replyTime;
    private String memberName;
    private String memberAvatar;
}
