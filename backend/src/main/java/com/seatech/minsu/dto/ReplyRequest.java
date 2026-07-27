package com.seatech.minsu.dto;

import lombok.Data;

/** 通用回复请求体 { replyContent } */
@Data
public class ReplyRequest {
    private String replyContent;
}
