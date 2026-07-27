package com.seatech.minsu.common;

import net.coobird.thumbnailator.Thumbnails;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * 图片压缩：保证落库图片不超过 {@link #MAX_STORED_BYTES}（15MB）。
 * 策略（推荐方案）：
 *  - 已符合上限的 JPG/PNG/GIF/WEBP：原样落库，零质量损失；
 *  - BMP 或超限的 JPG/PNG：等比缩放 + 质量递减重编码为 JPEG，直至 ≤ 上限（视觉无损）；
 *  - 超限的 GIF/WEBP：无法安全压缩（动图/透明/ImageIO 无解码器），拒绝并提示。
 * 纯 Java 实现（Thumbnailator + ImageIO），无原生依赖。
 */
public final class ImageCompressor {

    /** 落库上限 15MB */
    public static final long MAX_STORED_BYTES = 15L * 1024 * 1024;

    /** 超大分辨率兜底上限（长边像素）：超过则先等比缩小 */
    private static final int MAX_DIMENSION = 4096;

    /** 已符合上限时可原样保留的格式（BMP 除外，未压缩格式一律转 JPEG） */
    private static final Set<String> PASSTHROUGH_IF_OK = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private ImageCompressor() {
    }

    /** 压缩结果：落库字节与最终扩展名（含点，可能由 png/bmp 变为 .jpg） */
    public static final class Result {
        public final byte[] data;
        public final String ext;

        Result(byte[] data, String ext) {
            this.data = data;
            this.ext = ext;
        }
    }

    /**
     * @param src          原始图片字节
     * @param normalizedExt 小写扩展名（含点，如 ".png"）
     */
    public static Result compress(byte[] src, String normalizedExt) {
        String fmt = normalizedExt.startsWith(".") ? normalizedExt.substring(1) : normalizedExt;

        // 已符合上限的常见格式：原样落库，避免无谓的重编码质量损失
        if (src.length <= MAX_STORED_BYTES && PASSTHROUGH_IF_OK.contains(fmt)) {
            return new Result(src, "." + fmt);
        }

        // 超限的 GIF/WEBP：动图重编码会丢帧、WEBP 无内置解码器，无法安全压缩
        if ("gif".equals(fmt) || "webp".equals(fmt)) {
            throw new BusinessException("动图/WEBP 图片超过 15MB 且无法自动压缩，请改用更小的图片或 JPG/PNG 格式");
        }

        // jpg/jpeg/png/bmp → 重编码为 JPEG 并保证 ≤ 上限
        try {
            return new Result(compressToJpeg(src), ".jpg");
        } catch (IOException e) {
            throw new BusinessException("图片解析失败，请更换图片后重试");
        }
    }

    private static byte[] compressToJpeg(byte[] src) throws IOException {
        // 按 EXIF 方向解码为 BufferedImage（保留手机拍摄的旋转信息）
        BufferedImage decoded = Thumbnails.of(new ByteArrayInputStream(src))
                .scale(1.0)
                .useExifOrientation(true)
                .asBufferedImage();
        if (decoded == null) {
            throw new IOException("unreadable image");
        }

        // 展平透明通道到白底，避免 PNG→JPEG 出现黑/异色背景
        BufferedImage rgb = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(decoded, 0, 0, Color.WHITE, null);
        g.dispose();

        int maxDim = Math.max(rgb.getWidth(), rgb.getHeight());
        double scale = maxDim > MAX_DIMENSION ? (double) MAX_DIMENSION / maxDim : 1.0;
        double quality = 0.9;

        byte[] out = encodeJpeg(rgb, scale, quality);
        // 有界循环：先降质到 0.5，再按 0.85 逐步缩放；直到 ≤ 上限或尺寸过小兜底
        int guard = 0;
        while (out.length > MAX_STORED_BYTES && guard++ < 40) {
            if (quality > 0.5) {
                quality -= 0.1;
            } else {
                scale *= 0.85;
                quality = 0.85;
                if ((int) (maxDim * scale) < 320) {
                    break;
                }
            }
            out = encodeJpeg(rgb, scale, quality);
        }
        return out;
    }

    private static byte[] encodeJpeg(BufferedImage img, double scale, double quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(img)
                .scale(scale <= 0 ? 0.01 : scale)
                .outputFormat("jpg")
                .outputQuality(Math.max(0.1, Math.min(1.0, quality)))
                .toOutputStream(baos);
        return baos.toByteArray();
    }
}
