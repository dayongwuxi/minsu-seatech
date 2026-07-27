package com.seatech.minsu.controller.admin;

import com.seatech.minsu.common.Result;
import com.seatech.minsu.config.LlmProperties;
import com.seatech.minsu.dto.LlmChatHttpRequest;
import com.seatech.minsu.llm.LlmService;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmModelInfo;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LLM 网关管理端接口。均在 /api/admin/** 之下，复用现有 admin 鉴权拦截器。
 * - GET  /health      是否已配置 + 真实连通性
 * - GET  /models      当前角色→模型路由表
 * - POST /chat        非流式对话
 * - POST /chat/stream 流式对话（SSE，逐 token 推送）
 * 房间多语言文案生成在 AdminRoomController（/api/admin/rooms/{id}/i18n/**）。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/llm")
public class AdminLlmController {

    private final LlmService llmService;
    private final ExecutorService streamExecutor;

    public AdminLlmController(LlmService llmService, LlmProperties props) {
        this.llmService = llmService;
        this.streamExecutor = Executors.newFixedThreadPool(Math.max(2, props.getMaxConcurrency()));
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        boolean enabled = llmService.isEnabled();
        data.put("enabled", enabled);
        data.put("reachable", enabled && llmService.health());
        data.put("models", llmService.models());
        return Result.ok(data);
    }

    @GetMapping("/models")
    public Result<List<LlmModelInfo>> models() {
        return Result.ok(llmService.models());
    }

    @PostMapping("/chat")
    public Result<LlmChatResult> chat(@RequestBody LlmChatHttpRequest req) {
        return Result.ok(llmService.chat(req.getRole(), req.getMessages(), req.toOptions()));
    }

    @PostMapping("/chat/stream")
    public SseEmitter stream(@RequestBody LlmChatHttpRequest req) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时，由流结束决定
        streamExecutor.submit(() -> {
            try {
                llmService.streamChat(req.getRole(), req.getMessages(), req.toOptions(), token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        throw new RuntimeException(e); // 客户端断开，终止流
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("SSE 流式对话中断: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
    }
}
