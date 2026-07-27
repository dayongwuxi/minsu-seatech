package com.seatech.minsu.common;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 从 DB 任意字符串字段值中提取 /files/ 文件名引用的单元测试。
 * 覆盖：纯路径、完整 URL 前缀、逗号分隔多图（feedback.images）、富文本内嵌、尾随标点。
 */
class FileRefExtractorTest {

    @Test
    void extractsPlainPath() {
        assertEquals(Set.of("abc123.jpg"), FileRefExtractor.extract("/files/abc123.jpg"));
    }

    @Test
    void extractsFromFullUrl() {
        assertEquals(Set.of("a.png"), FileRefExtractor.extract("https://seabnb.axionintell.com/files/a.png"));
    }

    @Test
    void extractsCommaSeparatedMulti() {
        Set<String> refs = FileRefExtractor.extract("/files/a.jpg,/files/b.webp, /files/c.png");
        assertEquals(Set.of("a.jpg", "b.webp", "c.png"), refs);
    }

    @Test
    void extractsFromRichText() {
        Set<String> refs = FileRefExtractor.extract("<p>房间实拍<img src=\"/files/d1.jpg\"></p>见 /files/e2.png。");
        assertEquals(Set.of("d1.jpg", "e2.png"), refs);
    }

    @Test
    void stripsTrailingPunctuation() {
        // 句末的点不应进入文件名，否则引用比对失配 → 误删风险
        assertEquals(Set.of("a.jpg"), FileRefExtractor.extract("图见 /files/a.jpg."));
    }

    @Test
    void returnsEmptyForNullOrNoMatch() {
        assertTrue(FileRefExtractor.extract(null).isEmpty());
        assertTrue(FileRefExtractor.extract("").isEmpty());
        assertTrue(FileRefExtractor.extract("https://example.com/pic.jpg 无本站引用").isEmpty());
        assertTrue(FileRefExtractor.extract("/files/").isEmpty());
    }
}
