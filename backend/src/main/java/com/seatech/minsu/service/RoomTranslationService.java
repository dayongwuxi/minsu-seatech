package com.seatech.minsu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.dto.RoomCopyVO;
import com.seatech.minsu.dto.RoomI18nVO;
import com.seatech.minsu.entity.RoomI18n;
import com.seatech.minsu.llm.LlmService;
import com.seatech.minsu.llm.dto.LlmChatOptions;
import com.seatech.minsu.llm.dto.LlmChatResult;
import com.seatech.minsu.llm.dto.LlmMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 房间说明/预订须知的多语言翻译。base 中文 → 站点其余 8 语种，走 "translator" 角色
 * （默认 gemma4:31b），JSON Schema 强约束输出 {description, bookingNote}。
 * 并发翻译各语种，落库到 room_i18n（成功即覆盖对应语种，失败保留旧值）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomTranslationService {

    private static final String ROLE = "translator";

    /** 目标语言（base 中文除外），与用户端 SUPPORTED_LANGS 对齐 */
    static final Map<String, String> TARGET_LANGS = new LinkedHashMap<>();

    static {
        TARGET_LANGS.put("en", "English");
        TARGET_LANGS.put("ja", "日本語");
        TARGET_LANGS.put("ko", "한국어");
        TARGET_LANGS.put("fr", "Français");
        TARGET_LANGS.put("de", "Deutsch");
        TARGET_LANGS.put("es", "Español");
        TARGET_LANGS.put("pt", "Português");
        TARGET_LANGS.put("ru", "Русский");
    }

    private final LlmService llmService;
    private final RoomI18nService roomI18nService;
    private final ObjectMapper objectMapper;

    private ExecutorService pool;

    @PostConstruct
    void init() {
        this.pool = Executors.newFixedThreadPool(TARGET_LANGS.size());
    }

    @PreDestroy
    void shutdown() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    /** 读取房间现有多语言译文 */
    public List<RoomI18nVO> list(Long roomId) {
        return roomI18nService.lambdaQuery()
                .eq(RoomI18n::getRoomId, roomId)
                .list().stream()
                .map(r -> new RoomI18nVO(r.getLang(), r.getDescription(), r.getBookingNote()))
                .toList();
    }

    /**
     * 一键生成：把给定中文说明/须知翻译为全部目标语种并落库（成功即覆盖）。
     * @return 落库后的全部语种译文
     */
    public List<RoomI18nVO> generateAll(Long roomId, String zhDescription, String zhBookingNote) {
        if (!StringUtils.hasText(zhDescription) && !StringUtils.hasText(zhBookingNote)) {
            throw new BusinessException("请先填写中文房间说明或预订须知，再生成多语言版本");
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<String, String> e : TARGET_LANGS.entrySet()) {
            String lang = e.getKey();
            String langName = e.getValue();
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    RoomCopyVO t = translateOne(langName, zhDescription, zhBookingNote);
                    upsert(roomId, lang, t.getDescription(), t.getBookingNote());
                } catch (Exception ex) {
                    log.warn("翻译 {} 失败, 保留旧值: {}", lang, ex.getMessage());
                }
            }, pool));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return list(roomId);
    }

    /** 保存/覆盖单个语言版本（管理员手工编辑后） */
    public RoomI18nVO saveOne(Long roomId, String lang, String description, String bookingNote) {
        if (!TARGET_LANGS.containsKey(lang)) {
            throw new BusinessException("不支持的语言: " + lang);
        }
        upsert(roomId, lang, description, bookingNote);
        return new RoomI18nVO(lang, description, bookingNote);
    }

    public void deleteByRoom(Long roomId) {
        roomI18nService.lambdaUpdate().eq(RoomI18n::getRoomId, roomId).remove();
    }

    private RoomCopyVO translateOne(String langName, String zhDescription, String zhBookingNote) {
        LlmChatOptions options = new LlmChatOptions();
        options.setTemperature(0.3); // 翻译求稳，低温
        options.setFormat(outputSchema());

        String sys = "你是专业本地化翻译。把给定的中文房间说明和预订须知准确翻译为【" + langName + "】。"
                + "保持原意、语气与换行格式，不要增删信息，不要输出中文。只输出 JSON。";
        String user = "房间说明(description)：\n" + safe(zhDescription)
                + "\n\n预订须知(bookingNote)：\n" + safe(zhBookingNote);

        LlmChatResult r = llmService.chat(ROLE,
                List.of(LlmMessage.system(sys), LlmMessage.user(user)), options);
        try {
            return objectMapper.readValue(extractJson(r.getContent()), RoomCopyVO.class);
        } catch (Exception e) {
            throw new BusinessException("翻译结果解析失败");
        }
    }

    /** 容错提取 JSON：部分模型(如 gemma)会用 ```json 围栏包裹，需剥离后再截取 {...} */
    static String extractJson(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            t = nl >= 0 ? t.substring(nl + 1) : t.substring(3);
            int fence = t.lastIndexOf("```");
            if (fence >= 0) {
                t = t.substring(0, fence);
            }
            t = t.trim();
        }
        int a = t.indexOf('{');
        int b = t.lastIndexOf('}');
        if (a >= 0 && b > a) {
            t = t.substring(a, b + 1);
        }
        return t.trim();
    }

    private void upsert(Long roomId, String lang, String description, String bookingNote) {
        RoomI18n existing = roomI18nService.getOne(new LambdaQueryWrapper<RoomI18n>()
                .eq(RoomI18n::getRoomId, roomId).eq(RoomI18n::getLang, lang), false);
        RoomI18n row = existing == null ? new RoomI18n() : existing;
        row.setRoomId(roomId);
        row.setLang(lang);
        row.setDescription(description);
        row.setBookingNote(bookingNote);
        roomI18nService.saveOrUpdate(row);
    }

    private Map<String, Object> outputSchema() {
        Map<String, Object> str = Map.of("type", "string");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("description", str);
        props.put("bookingNote", str);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("description", "bookingNote"));
        return schema;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
