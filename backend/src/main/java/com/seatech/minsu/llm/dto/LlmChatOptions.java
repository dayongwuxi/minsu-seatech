package com.seatech.minsu.llm.dto;

import lombok.Data;

/** 单次对话的可选参数，映射到 Ollama /api/chat 的 options/format */
@Data
public class LlmChatOptions {

    /** 采样温度（options.temperature），null=用模型默认 */
    private Double temperature;

    /** 最大生成 token 数（options.num_predict），null=不限制 */
    private Integer maxTokens;

    /**
     * 结构化输出：传 "json" 或一个 JSON Schema（Map/ObjectNode），
     * 映射到 Ollama 的 format 字段，强约束模型只吐合法 JSON。
     */
    private Object format;

    public static LlmChatOptions of(Double temperature, Integer maxTokens) {
        LlmChatOptions o = new LlmChatOptions();
        o.temperature = temperature;
        o.maxTokens = maxTokens;
        return o;
    }
}
