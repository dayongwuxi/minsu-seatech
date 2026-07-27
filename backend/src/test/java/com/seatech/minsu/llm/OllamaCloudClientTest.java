package com.seatech.minsu.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatech.minsu.config.LlmProperties;
import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** OllamaCloudClient 的纯逻辑测试：请求构造 + 响应/流式解析（无需网络） */
class OllamaCloudClientTest {

    private final ObjectMapper om = new ObjectMapper();

    private OllamaCloudClient client() {
        LlmProperties props = new LlmProperties();
        props.setApiKey("test-key");
        return new OllamaCloudClient(props);
    }

    @Test
    void buildsRequestBodyWithOptionsAndFormat() throws Exception {
        LlmChatOptions opt = new LlmChatOptions();
        opt.setTemperature(0.7);
        opt.setMaxTokens(256);
        opt.setFormat("json");
        String body = client().buildRequestBody("gpt-oss:120b",
                List.of(LlmMessage.system("s"), LlmMessage.user("hi")), opt, false);

        var root = om.readTree(body);
        assertEquals("gpt-oss:120b", root.get("model").asText());
        assertFalse(root.get("stream").asBoolean());
        assertEquals(2, root.get("messages").size());
        assertEquals("hi", root.get("messages").get(1).get("content").asText());
        assertEquals(0.7, root.get("options").get("temperature").asDouble(), 1e-9);
        assertEquals(256, root.get("options").get("num_predict").asInt());
        assertEquals("json", root.get("format").asText());
    }

    @Test
    void buildsStreamRequestWithoutOptionsNode() throws Exception {
        String body = client().buildRequestBody("m", List.of(LlmMessage.user("hi")), null, true);
        var root = om.readTree(body);
        assertTrue(root.get("stream").asBoolean());
        assertFalse(root.has("options"));
        assertFalse(root.has("format"));
    }

    @Test
    void parsesNonStreamResponse() throws Exception {
        String json = """
                {"model":"gpt-oss:120b","message":{"role":"assistant","content":"你好"},
                 "done":true,"prompt_eval_count":12,"eval_count":34,"total_duration":2500000000}
                """;
        LlmChatResult r = OllamaCloudClient.parseChatResponse(json, om);
        assertEquals("gpt-oss:120b", r.getModel());
        assertEquals("你好", r.getContent());
        assertEquals(12, r.getPromptTokens());
        assertEquals(34, r.getEvalTokens());
        assertEquals(2500L, r.getTotalDurationMs()); // 2.5e9 ns → 2500 ms
    }

    @Test
    void parsesStreamContentAndDone() throws Exception {
        assertEquals("Hel", OllamaCloudClient.parseStreamContent(
                "{\"message\":{\"content\":\"Hel\"},\"done\":false}", om));
        assertFalse(OllamaCloudClient.isDone("{\"message\":{\"content\":\"Hel\"},\"done\":false}", om));
        assertTrue(OllamaCloudClient.isDone("{\"message\":{\"content\":\"\"},\"done\":true}", om));
        // 无 message.content 的行返回 null
        assertNull(OllamaCloudClient.parseStreamContent("{\"done\":true}", om));
    }
}
