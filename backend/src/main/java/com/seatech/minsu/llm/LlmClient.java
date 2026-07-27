package com.seatech.minsu.llm;

import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * LLM 供应商抽象（当前实现：Ollama Cloud）。
 * 与 {@code RateFetcher} 同构：接口在此，具体供应商实现可替换。
 */
public interface LlmClient {

    /** 非流式对话，返回完整结果 */
    LlmChatResult chat(String model, List<LlmMessage> messages, LlmChatOptions options) throws Exception;

    /** 流式对话，逐段回调正文增量（onToken 收到的是增量片段，非累积） */
    void streamChat(String model, List<LlmMessage> messages, LlmChatOptions options,
                    Consumer<String> onToken) throws Exception;

    /** 轻量连通性探测（用于 /health），失败返回 false 不抛出 */
    boolean ping(String model);
}
