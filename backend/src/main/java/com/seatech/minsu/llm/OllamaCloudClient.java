package com.seatech.minsu.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatech.minsu.config.LlmProperties;
import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ollama Cloud 客户端。基址 https://ollama.com，鉴权 Bearer；
 * 走原生 /api/chat（stream=false 一次性 / stream=true NDJSON 流式）。
 * 复用单例 HttpClient（线程安全、连接池、HTTP/2）。解析逻辑抽为可单测的静态方法。
 */
@Slf4j
@Component
public class OllamaCloudClient implements LlmClient {

    private final LlmProperties props;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaCloudClient(LlmProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public LlmChatResult chat(String model, List<LlmMessage> messages, LlmChatOptions options) throws Exception {
        String body = buildRequestBody(model, messages, options, false);
        HttpResponse<String> resp = httpClient.send(newRequest(body),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Ollama API HTTP " + resp.statusCode() + ": " + brief(resp.body()));
        }
        return parseChatResponse(resp.body(), objectMapper);
    }

    @Override
    public void streamChat(String model, List<LlmMessage> messages, LlmChatOptions options,
                           Consumer<String> onToken) throws Exception {
        String body = buildRequestBody(model, messages, options, true);
        HttpResponse<InputStream> resp = httpClient.send(newRequest(body),
                HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Ollama API HTTP " + resp.statusCode());
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String token = parseStreamContent(line, objectMapper);
                if (token != null && !token.isEmpty()) {
                    onToken.accept(token);
                }
                if (isDone(line, objectMapper)) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean ping(String model) {
        try {
            LlmChatOptions opt = new LlmChatOptions();
            opt.setMaxTokens(1);
            LlmChatResult r = chat(model, List.of(LlmMessage.user("ping")), opt);
            return r != null;
        } catch (Exception e) {
            log.warn("LLM ping 失败 [{}]: {}", model, e.getMessage());
            return false;
        }
    }

    private HttpRequest newRequest(String body) {
        return HttpRequest.newBuilder(URI.create(props.getBaseUrl() + "/api/chat"))
                .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    /** 构造 /api/chat 请求体 */
    String buildRequestBody(String model, List<LlmMessage> messages, LlmChatOptions options, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", stream);
        var msgs = root.putArray("messages");
        for (LlmMessage m : messages) {
            ObjectNode node = msgs.addObject();
            node.put("role", m.getRole());
            node.put("content", m.getContent());
        }
        if (options != null) {
            if (options.getTemperature() != null || options.getMaxTokens() != null) {
                ObjectNode opt = root.putObject("options");
                if (options.getTemperature() != null) {
                    opt.put("temperature", options.getTemperature());
                }
                if (options.getMaxTokens() != null) {
                    opt.put("num_predict", options.getMaxTokens());
                }
            }
            if (options.getFormat() != null) {
                root.set("format", objectMapper.valueToTree(options.getFormat()));
            }
        }
        return root.toString();
    }

    /** 解析非流式响应体 → 结果 */
    static LlmChatResult parseChatResponse(String json, ObjectMapper om) throws Exception {
        JsonNode root = om.readTree(json);
        LlmChatResult r = new LlmChatResult();
        r.setModel(root.path("model").asText(null));
        r.setContent(root.path("message").path("content").asText(""));
        if (root.hasNonNull("prompt_eval_count")) {
            r.setPromptTokens(root.get("prompt_eval_count").asInt());
        }
        if (root.hasNonNull("eval_count")) {
            r.setEvalTokens(root.get("eval_count").asInt());
        }
        if (root.hasNonNull("total_duration")) {
            r.setTotalDurationMs(root.get("total_duration").asLong() / 1_000_000L);
        }
        return r;
    }

    /** 解析一行流式 NDJSON 的正文增量；无正文返回 null */
    static String parseStreamContent(String line, ObjectMapper om) throws Exception {
        JsonNode root = om.readTree(line);
        JsonNode content = root.path("message").path("content");
        return content.isMissingNode() ? null : content.asText("");
    }

    static boolean isDone(String line, ObjectMapper om) throws Exception {
        return om.readTree(line).path("done").asBoolean(false);
    }

    private static String brief(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }
}
