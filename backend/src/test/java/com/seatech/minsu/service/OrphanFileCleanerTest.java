package com.seatech.minsu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 孤儿文件清扫纯逻辑的单元测试（不依赖 DB/Spring）。
 * 防误伤重点：被引用的不删、宽限期内的不删、非上传命名格式的不删、子目录不碰。
 */
class OrphanFileCleanerTest {

    private static final Instant NOW = Instant.parse("2026-07-23T12:00:00Z");
    private static final Duration GRACE = Duration.ofHours(48);
    /** 上传接口生成的文件名格式：32 位 hex + 扩展名 */
    private static final String OLD_ORPHAN = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg";
    private static final String OLD_REFERENCED = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png";
    private static final String NEW_ORPHAN = "cccccccccccccccccccccccccccccccc.webp";

    private static Path touch(Path dir, String name, Instant mtime) throws IOException {
        Path p = dir.resolve(name);
        Files.write(p, new byte[]{1, 2, 3});
        Files.setLastModifiedTime(p, FileTime.from(mtime));
        return p;
    }

    @Test
    void deletesOldOrphanOnly(@TempDir Path dir) throws IOException {
        Path oldOrphan = touch(dir, OLD_ORPHAN, NOW.minus(Duration.ofDays(10)));
        Path oldReferenced = touch(dir, OLD_REFERENCED, NOW.minus(Duration.ofDays(10)));
        Path newOrphan = touch(dir, NEW_ORPHAN, NOW.minus(Duration.ofHours(1)));

        OrphanFileCleaner.SweepResult r =
                OrphanFileCleaner.sweep(dir, Set.of(OLD_REFERENCED), GRACE, NOW);

        assertFalse(Files.exists(oldOrphan), "超宽限期的孤儿应被删除");
        assertTrue(Files.exists(oldReferenced), "被 DB 引用的文件绝不能删");
        assertTrue(Files.exists(newOrphan), "宽限期内的孤儿(可能正在编辑)不能删");
        assertEquals(List.of(OLD_ORPHAN), r.deleted());
        assertEquals(1, r.keptReferenced());
        assertEquals(1, r.keptRecent());
        assertEquals(3, r.deletedBytes());
    }

    @Test
    void keepsFileExactlyAtGraceBoundary(@TempDir Path dir) throws IOException {
        // 边界：恰好等于宽限期视为"未超过"，不删
        Path p = touch(dir, OLD_ORPHAN, NOW.minus(GRACE));
        OrphanFileCleaner.SweepResult r = OrphanFileCleaner.sweep(dir, Set.of(), GRACE, NOW);
        assertTrue(Files.exists(p));
        assertTrue(r.deleted().isEmpty());
    }

    @Test
    void neverTouchesForeignNamedFiles(@TempDir Path dir) throws IOException {
        // 非上传接口命名格式的文件（人工放置/其他来源），无论多老都不删
        Path readme = touch(dir, "README.txt", NOW.minus(Duration.ofDays(365)));
        Path manual = touch(dir, "banner-final.jpg", NOW.minus(Duration.ofDays(365)));
        Path shortHex = touch(dir, "abc123.jpg", NOW.minus(Duration.ofDays(365)));

        OrphanFileCleaner.SweepResult r = OrphanFileCleaner.sweep(dir, Set.of(), GRACE, NOW);

        assertTrue(Files.exists(readme));
        assertTrue(Files.exists(manual));
        assertTrue(Files.exists(shortHex));
        assertTrue(r.deleted().isEmpty());
        assertEquals(3, r.skippedForeign());
    }

    @Test
    void ignoresSubdirectories(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Path inSub = touch(sub, OLD_ORPHAN, NOW.minus(Duration.ofDays(10)));

        OrphanFileCleaner.SweepResult r = OrphanFileCleaner.sweep(dir, Set.of(), GRACE, NOW);

        assertTrue(Files.exists(inSub), "子目录内容不在清理范围");
        assertTrue(r.deleted().isEmpty());
    }

    @Test
    void handlesMissingDirGracefully(@TempDir Path dir) {
        OrphanFileCleaner.SweepResult r =
                OrphanFileCleaner.sweep(dir.resolve("not-exist"), Set.of(), GRACE, NOW);
        assertTrue(r.deleted().isEmpty());
        assertEquals(0, r.keptReferenced());
    }
}
