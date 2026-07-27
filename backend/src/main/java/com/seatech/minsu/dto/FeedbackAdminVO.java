package com.seatech.minsu.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端投诉反馈列表项 */
@Data
public class FeedbackAdminVO {
    private Long id;
    private String feedbackNo;
    private Long memberId;
    private Integer type;
    private String title;
    private String content;
    private String images;
    private Integer status;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private String memberName;
    private String memberUsername;
    private String memberPhone;
}
