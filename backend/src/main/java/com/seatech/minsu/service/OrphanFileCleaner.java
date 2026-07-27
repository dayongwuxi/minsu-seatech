package com.seatech.minsu.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 上传目录孤儿文件清扫（纯逻辑，不触 DB）。
 * 只删同时满足三个条件的文件：未被引用、mtime 超过宽限期、
 * 且文件名是上传接口生成的 32 位 hex+扩展名格式——其他来源的文件一律不碰。
 */
public final class OrphanFileCleaner {

    /** UploadController 生成的文件名：UUID 去横线(32位hex) + 小写扩展名 */
    private static final Pattern UPLOADED_NAME = Pattern.compile("^[0-9a-f]{32}\\.[a-z0-9]{2,5}$");

    private OrphanFileCleaner() {
    }

    public record SweepResult(List<String> deleted, long deletedBytes,
                              int keptReferenced, int keptRecent, int skippedForeign) {
    }

    public static SweepResult sweep(Path uploadDir, Set<String> referenced, Duration grace, Instant now) {
        List<String> deleted = new ArrayList<>();
        long deletedBytes = 0;
        int keptReferenced = 0;
        int keptRecent = 0;
        int skippedForeign = 0;
        if (!Files.isDirectory(uploadDir)) {
            return new SweepResult(deleted, 0, 0, 0, 0);
        }
        Instant cutoff = now.minus(grace);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (!UPLOADED_NAME.matcher(name).matches()) {
                    skippedForeign++;
                    continue;
                }
                if (referenced.contains(name)) {
                    keptReferenced++;
                    continue;
                }
                Instant mtime = Files.getLastModifiedTime(p).toInstant();
                if (!mtime.isBefore(cutoff)) {
                    keptRecent++;
                    continue;
                }
                long size = Files.size(p);
                Files.delete(p);
                deleted.add(name);
                deletedBytes += size;
            }
        } catch (IOException e) {
            throw new IllegalStateException("清扫上传目录失败: " + uploadDir, e);
        }
        return new SweepResult(deleted, deletedBytes, keptReferenced, keptRecent, skippedForeign);
    }
}
