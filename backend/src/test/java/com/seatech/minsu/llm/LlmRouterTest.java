package com.seatech.minsu.llm;

import com.seatech.minsu.config.LlmProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmRouterTest {

    private LlmRouter router(Map<String, String> models, String defaultRole, String fallback) {
        LlmProperties props = new LlmProperties();
        props.setModels(models);
        props.setDefaultRole(defaultRole);
        props.setFallbackModel(fallback);
        return new LlmRouter(props);
    }

    private Map<String, String> models() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("default", "gpt-oss:120b");
        m.put("fast", "gpt-oss:20b");
        m.put("copywriter", "gpt-oss:120b");
        return m;
    }

    @Test
    void resolvesExactRole() {
        assertEquals("gpt-oss:20b", router(models(), "default", "").resolve("fast"));
    }

    @Test
    void unknownRoleFallsBackToDefaultRole() {
        assertEquals("gpt-oss:120b", router(models(), "default", "").resolve("nope"));
        assertEquals("gpt-oss:120b", router(models(), "default", "").resolve(null));
    }

    @Test
    void whenDefaultRoleMissingUsesFirstEntry() {
        assertEquals("gpt-oss:120b", router(models(), "absent", "").resolve("nope"));
    }

    @Test
    void whenNoModelsConfiguredUsesFallbackModel() {
        assertEquals("gpt-oss:20b", router(new LinkedHashMap<>(), "default", "gpt-oss:20b").resolve("x"));
    }

    @Test
    void listReflectsRoutingTable() {
        assertEquals(3, router(models(), "default", "").list().size());
    }
}
