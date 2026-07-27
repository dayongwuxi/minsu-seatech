package com.seatech.minsu.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片地址兜底校验：只接受本站上传接口产生的相对地址（{@link #PREFIX} 开头），
 * 过滤掉 blob: 临时预览地址、绝对 URL、空值，防止前端把无效地址写入库。
 */
public final class ImageUrls {

    /** 与 UploadController 落库地址一致的可访问前缀 */
    public static final String PREFIX = "/files/";

    private ImageUrls() {
    }

    /** 返回仅含合法 /files/ 地址的新列表，保持原顺序；入参为 null 时返回空列表 */
    public static List<String> sanitize(List<String> urls) {
        List<String> out = new ArrayList<>();
        if (urls == null) {
            return out;
        }
        for (String u : urls) {
            if (u != null && u.startsWith(PREFIX)) {
                out.add(u);
            }
        }
        return out;
    }
}
