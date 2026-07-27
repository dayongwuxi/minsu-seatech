package com.seatech.minsu.dto;

import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmMessage;
import lombok.Data;

import java.util.List;

/** 管理端通用对话请求（/api/admin/llm/chat 与 /chat/stream 共用） */
@Data
public class LlmChatHttpRequest {
    /** 逻辑角色（default/fast/reasoning/copywriter…），留空走默认 */
    private String role;
    private List<LlmMessage> messages;
    private Double temperature;
    private Integer maxTokens;

    public LlmChatOptions toOptions() {
        LlmChatOptions o = new LlmChatOptions();
        o.setTemperature(temperature);
        o.setMaxTokens(maxTokens);
        return o;
    }
}
