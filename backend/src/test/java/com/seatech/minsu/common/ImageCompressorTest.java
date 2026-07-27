package com.seatech.minsu.common;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageCompressorTest {

    /** 生成高熵图（近乎不可压缩），用于制造超大 PNG 与验证压缩兜底 */
    private static BufferedImage noiseImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 整数哈希混合，产出近似随机的高熵像素，使 PNG 几乎无法压缩
                int hsh = x * 374761393 + y * 668265263;
                hsh = (hsh ^ (hsh >>> 13)) * 1274126177;
                hsh ^= (hsh >>> 16);
                img.setRGB(x, y, hsh & 0xFFFFFF);
            }
        }
        return img;
    }

    private static byte[] encode(BufferedImage img, String fmt) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, fmt, bos);
        return bos.toByteArray();
    }

    @Test
    void oversizePng_isCompressedUnderCap() throws IOException {
        byte[] png = encode(noiseImage(4500, 4500), "png");
        assertTrue(png.length > ImageCompressor.MAX_STORED_BYTES,
                "测试前提：原始 PNG 应 >15MB，实际 " + png.length);

        ImageCompressor.Result r = ImageCompressor.compress(png, ".png");

        assertEquals(".jpg", r.ext);
        assertTrue(r.data.length <= ImageCompressor.MAX_STORED_BYTES,
                "压缩后应 ≤15MB，实际 " + r.data.length);
        assertNotNull(ImageIO.read(new ByteArrayInputStream(r.data)), "输出应为可解码的有效图片");
    }

    @Test
    void smallJpeg_passesThroughUnchanged() throws IOException {
        byte[] jpg = encode(noiseImage(200, 200), "jpg");
        assertTrue(jpg.length <= ImageCompressor.MAX_STORED_BYTES);

        ImageCompressor.Result r = ImageCompressor.compress(jpg, ".jpg");

        assertEquals(".jpg", r.ext);
        assertSame(jpg, r.data, "已达标的 JPG 应原样返回，避免二次质量损失");
    }

    @Test
    void smallPng_keepsPngUnchanged() throws IOException {
        byte[] png = encode(noiseImage(200, 200), "png");
        assertTrue(png.length <= ImageCompressor.MAX_STORED_BYTES);

        ImageCompressor.Result r = ImageCompressor.compress(png, ".png");

        assertEquals(".png", r.ext, "已达标的 PNG 应保留原格式（保留透明通道）");
        assertSame(png, r.data);
    }

    @Test
    void bmp_isAlwaysConvertedToJpeg() throws IOException {
        byte[] bmp = encode(noiseImage(300, 300), "bmp");

        ImageCompressor.Result r = ImageCompressor.compress(bmp, ".bmp");

        assertEquals(".jpg", r.ext, "BMP（未压缩格式）应统一转 JPEG");
        assertTrue(r.data.length < bmp.length);
        assertNotNull(ImageIO.read(new ByteArrayInputStream(r.data)));
    }

    @Test
    void oversizeGif_isRejected() {
        byte[] fake = new byte[(int) ImageCompressor.MAX_STORED_BYTES + 1];
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ImageCompressor.compress(fake, ".gif"));
        assertTrue(ex.getMessage().contains("无法自动压缩"));
    }
}
