package com.seatech.minsu.llm.dto;

import lombok.Data;

/** 非流式对话结果：正文 + 用量/耗时（用于观测与计费核对） */
@Data
public class LlmChatResult {

    private String content;
    private String model;
    private Integer promptTokens;
    private Integer evalTokens;
    private Long totalDurationMs;
}
