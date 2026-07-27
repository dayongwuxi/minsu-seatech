package com.seatech.minsu.llm;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.config.LlmProperties;
import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;
import com.seatech.minsu.llm.dto.LlmModelInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * LLM 网关：对业务侧的统一入口。
 * 职责：启用校验（未配 key 即禁用）、角色路由、并发保护（信号量）、
 * 主模型失败自动降级、统一异常包装。业务只依赖本类，不直接碰供应商客户端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmProperties props;
    private final LlmClient client;
    private final LlmRouter router;

    private Semaphore semaphore;

    @PostConstruct
    void init() {
        this.semaphore = new Semaphore(Math.max(1, props.getMaxConcurrency()));
    }

    /** 是否已配置可用（有 API Key） */
    public boolean isEnabled() {
        return StringUtils.hasText(props.getApiKey());
    }

    public List<LlmModelInfo> models() {
        return router.list();
    }

    /** 连通性探测：未启用直接 false，否则真实 ping 默认模型 */
    public boolean health() {
        if (!isEnabled()) {
            return false;
        }
        return client.ping(router.resolve(props.getDefaultRole()));
    }

    /** 非流式对话（按角色路由，失败自动降级） */
    public LlmChatResult chat(String role, List<LlmMessage> messages, LlmChatOptions options) {
        ensureEnabled();
        validateMessages(messages);
        acquire();
        try {
            String model = router.resolve(role);
            try {
                return client.chat(model, messages, options);
            } catch (Exception primary) {
                String fb = props.getFallbackModel();
                if (StringUtils.hasText(fb) && !fb.equals(model)) {
                    log.warn("模型 {} 调用失败, 降级到 {}: {}", model, fb, primary.getMessage());
                    try {
                        return client.chat(fb, messages, options);
                    } catch (Exception secondary) {
                        throw wrap(secondary);
                    }
                }
                throw wrap(primary);
            }
        } finally {
            semaphore.release();
        }
    }

    /** 流式对话：同步阻塞逐段回调（SSE 由调用方在独立线程驱动） */
    public void streamChat(String role, List<LlmMessage> messages, LlmChatOptions options,
                           Consumer<String> onToken) {
        ensureEnabled();
        validateMessages(messages);
        acquire();
        try {
            String model = router.resolve(role);
            client.streamChat(model, messages, options, onToken);
        } catch (Exception e) {
            throw wrap(e);
        } finally {
            semaphore.release();
        }
    }

    private void ensureEnabled() {
        if (!isEnabled()) {
            throw new BusinessException("AI 服务未配置，请先设置 OLLAMA_API_KEY");
        }
    }

    private void validateMessages(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("对话内容不能为空");
        }
    }

    private void acquire() {
        try {
            if (!semaphore.tryAcquire(props.getAcquireTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "AI 服务繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SERVER_ERROR, "AI 服务被中断");
        }
    }

    private BusinessException wrap(Exception e) {
        log.warn("LLM 调用失败: {}", e.getMessage());
        return new BusinessException(ResultCode.SERVER_ERROR, "AI 生成失败，请稍后重试");
    }
}
