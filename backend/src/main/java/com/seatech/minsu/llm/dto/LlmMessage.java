package com.seatech.minsu.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 一条对话消息（role: system/user/assistant） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessage {

    private String role;
    private String content;

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content);
    }
}
