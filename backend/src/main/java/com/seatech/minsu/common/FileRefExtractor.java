package com.seatech.minsu.common;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 DB 字符串字段值中提取本站 /files/ 文件名引用。
 * 兼容纯路径、完整 URL 前缀、逗号分隔多图、富文本内嵌；文件名以字母数字收尾，
 * 避免把句末标点算进文件名导致引用比对失配。
 */
public final class FileRefExtractor {

    private static final Pattern REF = Pattern.compile("/files/([A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]|[A-Za-z0-9])");

    private FileRefExtractor() {
    }

    public static Set<String> extract(String value) {
        Set<String> refs = new HashSet<>();
        if (value == null || value.isEmpty()) {
            return refs;
        }
        Matcher m = REF.matcher(value);
        while (m.find()) {
            refs.add(m.group(1));
        }
        return refs;
    }
}
