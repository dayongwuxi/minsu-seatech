package com.seatech.minsu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 网关配置（Ollama Cloud）。密钥经环境变量 OLLAMA_API_KEY 注入，
 * 留空即视为「未配置/禁用」（与 Stripe 空密钥=Mock 一致）。
 *
 * 路由：role → 具体云模型名，业务侧按逻辑角色请求，可改配置换模型而不动代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minsu.llm")
public class LlmProperties {

    /** Ollama Cloud API 基址 */
    private String baseUrl = "https://ollama.com";

    /** API Key（Bearer），经 OLLAMA_API_KEY 注入；留空=禁用 */
    private String apiKey = "";

    /** 连接超时（毫秒） */
    private long connectTimeoutMs = 5000;

    /** 单次请求超时（毫秒），流式对话可适当放大 */
    private long requestTimeoutMs = 60000;

    /** 网关最大并发（信号量），保护后端与账单 */
    private int maxConcurrency = 8;

    /** 获取并发许可的最长等待（毫秒） */
    private long acquireTimeoutMs = 3000;

    /** 角色→模型 路由表（可在 application.yml / 环境变量覆盖） */
    private Map<String, String> models = new LinkedHashMap<>();

    /** 未命中路由时使用的默认角色 */
    private String defaultRole = "default";

    /** 主模型调用失败时的降级模型（留空=不降级） */
    private String fallbackModel = "";
}
