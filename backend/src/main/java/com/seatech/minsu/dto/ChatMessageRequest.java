package com.seatech.minsu.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long sessionId;
    /** 1文本 2图片 3文件，默认1 */
    private Integer contentType;
    private String content;
}
