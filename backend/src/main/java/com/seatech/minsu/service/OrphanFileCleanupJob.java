package com.seatech.minsu.service;

import com.seatech.minsu.common.FileRefExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 孤儿图片定时清理：启动 5 分钟后跑一次，之后每 24 小时一次。
 * 引用扫描覆盖当前库所有表的所有字符串列（information_schema 动态发现），
 * 任何扫描异常或结果可疑（无字符串列/引用集为空）都整体中止，不删任何文件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupJob {

    private static final String STRING_COLUMNS_SQL =
            "SELECT CONCAT(table_name, '.', column_name) FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() "
                    + "AND data_type IN ('char','varchar','tinytext','text','mediumtext','longtext')";

    private final JdbcTemplate jdbc;

    @Value("${minsu.upload.dir}")
    private String uploadDir;

    @Value("${minsu.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${minsu.cleanup.grace-hours:48}")
    private long graceHours;

    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void scheduledCleanup() {
        cleanupOnce();
    }

    /** 执行一次清理；被禁用、扫描失败或结果可疑时返回 null 且不删任何文件 */
    public OrphanFileCleaner.SweepResult cleanupOnce() {
        if (!enabled) {
            return null;
        }
        Set<String> referenced;
        try {
            referenced = collectReferencedNames();
        } catch (Exception e) {
            log.error("孤儿图片清理中止：DB 引用扫描失败，不删除任何文件", e);
            return null;
        }
        if (referenced.isEmpty()) {
            log.warn("孤儿图片清理中止：引用集为空（疑似扫描异常），不删除任何文件");
            return null;
        }
        OrphanFileCleaner.SweepResult r = OrphanFileCleaner.sweep(
                Paths.get(uploadDir), referenced, Duration.ofHours(graceHours), Instant.now());
        log.info("孤儿图片清理完成：删除 {} 个/{} KB，保留 引用中 {}、宽限期内 {}、非上传格式 {}；已删: {}",
                r.deleted().size(), r.deletedBytes() / 1024,
                r.keptReferenced(), r.keptRecent(), r.skippedForeign(), r.deleted());
        return r;
    }

    /** 扫当前库所有字符串列，收集全部 /files/ 引用文件名；无字符串列视为异常 */
    private Set<String> collectReferencedNames() {
        List<String> columns = jdbc.queryForList(STRING_COLUMNS_SQL, String.class);
        if (columns.isEmpty()) {
            throw new IllegalStateException("information_schema 未发现任何字符串列");
        }
        Set<String> referenced = new HashSet<>();
        for (String tc : columns) {
            int dot = tc.indexOf('.');
            String table = tc.substring(0, dot);
            String column = tc.substring(dot + 1);
            List<String> values = jdbc.queryForList(
                    "SELECT `" + column + "` FROM `" + table + "` WHERE `" + column + "` LIKE '%/files/%'",
                    String.class);
            for (String v : values) {
                referenced.addAll(FileRefExtractor.extract(v));
            }
        }
        return referenced;
    }
}
