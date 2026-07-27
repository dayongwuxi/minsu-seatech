package com.seatech.minsu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.dto.RoomI18nVO;
import com.seatech.minsu.entity.RoomI18n;
import com.seatech.minsu.llm.LlmService;
import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomTranslationServiceTest {

    private final ObjectMapper om = new ObjectMapper();
    private LlmService llm;
    private RoomI18nService i18nService;
    private RoomTranslationService svc;

    @BeforeEach
    void setUp() {
        llm = mock(LlmService.class);
        i18nService = mock(RoomI18nService.class);
        svc = new RoomTranslationService(llm, i18nService, om);
        svc.init();
        // upsert 里的查询链与保存都打桩
        when(i18nService.getOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(i18nService.saveOrUpdate(any(RoomI18n.class))).thenReturn(true);
    }

    private void stubList(List<RoomI18n> rows) {
        LambdaQueryChainWrapper<RoomI18n> chain = mock(LambdaQueryChainWrapper.class);
        when(i18nService.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.list()).thenReturn(rows);
    }

    @Test
    void generatesForAllEightTargetLanguagesAndPersists() {
        LlmChatResult r = new LlmChatResult();
        r.setContent("{\"description\":\"desc\",\"bookingNote\":\"note\"}");
        when(llm.chat(eq("translator"), any(), any())).thenReturn(r);
        stubList(List.of()); // 结果读取（此处只关心调用次数）

        svc.generateAll(1L, "中文说明", "中文须知");

        // 8 个目标语种各一次翻译调用 + 8 次落库
        verify(llm, times(8)).chat(eq("translator"), any(), any());
        verify(i18nService, times(8)).saveOrUpdate(any(RoomI18n.class));
    }

    @Test
    void translationPromptCarriesTargetLanguageAndUsesJsonSchema() {
        LlmChatResult r = new LlmChatResult();
        r.setContent("{\"description\":\"d\",\"bookingNote\":\"n\"}");
        when(llm.chat(eq("translator"), any(), any())).thenReturn(r);
        stubList(List.of());

        svc.generateAll(1L, "海景房", "14点入住");

        ArgumentCaptor<List<LlmMessage>> msgs = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LlmChatOptions> opts = ArgumentCaptor.forClass(LlmChatOptions.class);
        verify(llm, atLeastOnce()).chat(eq("translator"), msgs.capture(), opts.capture());
        // 至少有一次提示词提到某个目标语言名，且源文进入了 user 消息
        String all = msgs.getAllValues().toString();
        assertTrue(all.contains("English") || all.contains("日本語"));
        assertTrue(all.contains("海景房"));
        assertTrue(opts.getValue().getFormat() != null); // JSON schema 结构化
    }

    @Test
    void partialFailureIsSwallowedOthersStillPersist() {
        // 让翻译总是抛错：不应冒泡，落库次数为 0，但方法不抛
        when(llm.chat(eq("translator"), any(), any())).thenThrow(new RuntimeException("boom"));
        stubList(List.of());

        svc.generateAll(1L, "x", "y"); // 不抛异常
        verify(i18nService, times(0)).saveOrUpdate(any(RoomI18n.class));
    }

    @Test
    void rejectsEmptySource() {
        assertThrows(BusinessException.class, () -> svc.generateAll(1L, "  ", null));
    }

    @Test
    void saveOneRejectsUnsupportedLang() {
        assertThrows(BusinessException.class, () -> svc.saveOne(1L, "zz", "d", "n"));
    }

    @Test
    void extractJsonStripsMarkdownFenceAndProse() {
        // gemma 常见：```json 围栏
        assertEquals("{\"a\":1}", RoomTranslationService.extractJson("```json\n{\"a\":1}\n```"));
        // 无语言标注的围栏
        assertEquals("{\"a\":1}", RoomTranslationService.extractJson("```\n{\"a\":1}\n```"));
        // 纯 JSON 原样
        assertEquals("{\"a\":1}", RoomTranslationService.extractJson("{\"a\":1}"));
        // JSON 外有多余文字
        assertEquals("{\"a\":1}", RoomTranslationService.extractJson("这是结果：{\"a\":1} 完毕"));
    }

    @Test
    void translatesFencedJsonFromGemmaSuccessfully() {
        LlmChatResult r = new LlmChatResult();
        r.setContent("```json\n{\"description\":\"desc\",\"bookingNote\":\"note\"}\n```");
        when(llm.chat(eq("translator"), any(), any())).thenReturn(r);
        stubList(List.of());

        svc.generateAll(1L, "中文", "须知"); // 不应因围栏解析失败
        verify(i18nService, times(8)).saveOrUpdate(any(RoomI18n.class));
    }

    @Test
    void saveOnePersistsSupportedLang() {
        RoomI18nVO vo = svc.saveOne(1L, "ja", "説明", "ご案内");
        assertEquals("ja", vo.getLang());
        assertEquals("説明", vo.getDescription());
        verify(i18nService).saveOrUpdate(any(RoomI18n.class));
    }
}
