package com.seatech.minsu.llm;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.config.LlmProperties;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** LlmService：禁用态、路由、降级、并发保护 */
class LlmServiceTest {

    private LlmProperties props(String apiKey, String fallback) {
        LlmProperties p = new LlmProperties();
        p.setApiKey(apiKey);
        p.setFallbackModel(fallback);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("default", "gpt-oss:120b");
        m.put("fast", "gpt-oss:20b");
        p.setModels(m);
        p.setDefaultRole("default");
        return p;
    }

    private LlmService service(LlmProperties p, LlmClient client) {
        LlmService s = new LlmService(p, client, new LlmRouter(p));
        s.init();
        return s;
    }

    private LlmChatResult result(String content) {
        LlmChatResult r = new LlmChatResult();
        r.setContent(content);
        return r;
    }

    @Test
    void disabledWhenNoApiKey() {
        LlmService s = service(props("", ""), mock(LlmClient.class));
        assertEquals(false, s.isEnabled());
        assertThrows(BusinessException.class,
                () -> s.chat("default", List.of(LlmMessage.user("hi")), null));
    }

    @Test
    void routesRoleToConfiguredModel() throws Exception {
        LlmClient client = mock(LlmClient.class);
        when(client.chat(eq("gpt-oss:20b"), any(), any())).thenReturn(result("ok"));
        LlmService s = service(props("k", ""), client);

        assertEquals("ok", s.chat("fast", List.of(LlmMessage.user("hi")), null).getContent());
        verify(client).chat(eq("gpt-oss:20b"), any(), any());
    }

    @Test
    void fallsBackToFallbackModelWhenPrimaryFails() throws Exception {
        LlmClient client = mock(LlmClient.class);
        when(client.chat(eq("gpt-oss:120b"), any(), any())).thenThrow(new RuntimeException("boom"));
        when(client.chat(eq("gpt-oss:20b"), any(), any())).thenReturn(result("recovered"));
        LlmService s = service(props("k", "gpt-oss:20b"), client);

        assertEquals("recovered", s.chat("default", List.of(LlmMessage.user("hi")), null).getContent());
        verify(client).chat(eq("gpt-oss:20b"), any(), any());
    }

    @Test
    void wrapsErrorWhenNoFallback() throws Exception {
        LlmClient client = mock(LlmClient.class);
        when(client.chat(any(), any(), any())).thenThrow(new RuntimeException("boom"));
        LlmService s = service(props("k", ""), client);

        assertThrows(BusinessException.class,
                () -> s.chat("default", List.of(LlmMessage.user("hi")), null));
    }

    @Test
    void rejectsEmptyMessages() {
        LlmService s = service(props("k", ""), mock(LlmClient.class));
        assertThrows(BusinessException.class, () -> s.chat("default", List.of(), null));
    }
}
