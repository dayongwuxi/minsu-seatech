package com.seatech.minsu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 清理任务编排层测试：DB 扫描失败/可疑时必须整体中止（一个文件都不删），
 * 正常路径只删「超宽限期且未被任何字符串列引用」的上传文件。
 */
class OrphanFileCleanupJobTest {

    private static final String OLD_ORPHAN = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg";
    private static final String OLD_REFERENCED = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.png";

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private OrphanFileCleanupJob job(Path dir) {
        OrphanFileCleanupJob job = new OrphanFileCleanupJob(jdbc);
        ReflectionTestUtils.setField(job, "uploadDir", dir.toString());
        ReflectionTestUtils.setField(job, "enabled", true);
        ReflectionTestUtils.setField(job, "graceHours", 48L);
        return job;
    }

    private static Path oldFile(Path dir, String name) throws IOException {
        Path p = dir.resolve(name);
        Files.write(p, new byte[]{1});
        Files.setLastModifiedTime(p, FileTime.from(Instant.now().minus(Duration.ofDays(10))));
        return p;
    }

    @Test
    void deletesOnlyUnreferencedExpiredFiles(@TempDir Path dir) throws IOException {
        Path orphan = oldFile(dir, OLD_ORPHAN);
        Path referenced = oldFile(dir, OLD_REFERENCED);
        when(jdbc.queryForList(startsWith("SELECT CONCAT"), eq(String.class)))
                .thenReturn(List.of("room.cover_image", "feedback.images"));
        when(jdbc.queryForList(contains("`room`"), eq(String.class)))
                .thenReturn(List.of("/files/" + OLD_REFERENCED));
        when(jdbc.queryForList(contains("`feedback`"), eq(String.class)))
                .thenReturn(List.of());

        OrphanFileCleaner.SweepResult r = job(dir).cleanupOnce();

        assertFalse(Files.exists(orphan));
        assertTrue(Files.exists(referenced));
        assertEquals(List.of(OLD_ORPHAN), r.deleted());
    }

    @Test
    void abortsWhenDbScanFails(@TempDir Path dir) throws IOException {
        Path orphan = oldFile(dir, OLD_ORPHAN);
        when(jdbc.queryForList(startsWith("SELECT CONCAT"), eq(String.class)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        OrphanFileCleaner.SweepResult r = job(dir).cleanupOnce();

        assertNull(r, "扫描失败必须中止");
        assertTrue(Files.exists(orphan), "扫描失败时一个文件都不能删");
    }

    @Test
    void abortsWhenNoStringColumnsFound(@TempDir Path dir) throws IOException {
        // information_schema 返回空 = 扫描本身坏了，宁可不清也不冒险
        Path orphan = oldFile(dir, OLD_ORPHAN);
        when(jdbc.queryForList(startsWith("SELECT CONCAT"), eq(String.class)))
                .thenReturn(List.of());

        assertNull(job(dir).cleanupOnce());
        assertTrue(Files.exists(orphan));
    }

    @Test
    void abortsWhenZeroReferencesCollected(@TempDir Path dir) throws IOException {
        // 线上恒有封面图等引用，引用集为空高度可疑 → 中止
        Path orphan = oldFile(dir, OLD_ORPHAN);
        when(jdbc.queryForList(startsWith("SELECT CONCAT"), eq(String.class)))
                .thenReturn(List.of("room.cover_image"));
        when(jdbc.queryForList(contains("`room`"), eq(String.class)))
                .thenReturn(List.of());

        assertNull(job(dir).cleanupOnce());
        assertTrue(Files.exists(orphan));
    }

    @Test
    void disabledSkipsEverything(@TempDir Path dir) throws IOException {
        Path orphan = oldFile(dir, OLD_ORPHAN);
        OrphanFileCleanupJob job = job(dir);
        ReflectionTestUtils.setField(job, "enabled", false);

        assertNull(job.cleanupOnce());
        assertTrue(Files.exists(orphan));
        verifyNoInteractions(jdbc);
    }
}
