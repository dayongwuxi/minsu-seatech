package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.dto.UploadVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传接口的格式校验与压缩接线的单元测试（直接实例化，绕过鉴权拦截器）。
 * 覆盖用户反馈第 1 项（上传失败/格式限制）与压缩落地（第 15MB 上限）的接线。
 */
class UploadControllerTest {

    private final UploadController controller = new UploadController();

    private void useDir(Path dir) {
        ReflectionTestUtils.setField(controller, "uploadDir", dir.toString());
    }

    private static byte[] image(String fmt) throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, fmt, bos);
        return bos.toByteArray();
    }

    @Test
    void rejectsNonImageExtension(@TempDir Path dir) {
        useDir(dir);
        MockMultipartFile f = new MockMultipartFile("file", "evil.txt", "text/plain", "hello".getBytes());
        BusinessException ex = assertThrows(BusinessException.class, () -> controller.upload(f));
        assertTrue(ex.getMessage().contains("仅支持"));
    }

    @Test
    void rejectsImageExtensionWithNonImageContentType(@TempDir Path dir) throws IOException {
        useDir(dir);
        // 扩展名伪装成 .jpg，但 content-type 不是 image/*
        MockMultipartFile f = new MockMultipartFile("file", "fake.jpg", "application/octet-stream", image("jpg"));
        assertThrows(BusinessException.class, () -> controller.upload(f));
    }

    @Test
    void rejectsEmptyFile(@TempDir Path dir) {
        useDir(dir);
        MockMultipartFile f = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[0]);
        assertThrows(BusinessException.class, () -> controller.upload(f));
    }

    @Test
    void savesValidPngAsPassthrough(@TempDir Path dir) throws IOException {
        useDir(dir);
        byte[] png = image("png");
        MockMultipartFile f = new MockMultipartFile("file", "room.png", "image/png", png);

        Result<UploadVO> r = controller.upload(f);
        String url = r.getData().getUrl();

        assertTrue(url.startsWith("/files/") && url.endsWith(".png"), "URL 应为 /files/*.png，实际 " + url);
        Path saved = dir.resolve(url.substring("/files/".length()));
        assertTrue(Files.exists(saved));
        // ≤15MB 的 PNG 应原样落库（零质量损失）
        assertArrayEquals(png, Files.readAllBytes(saved));
    }

    @Test
    void convertsBmpToJpeg(@TempDir Path dir) throws IOException {
        useDir(dir);
        MockMultipartFile f = new MockMultipartFile("file", "room.bmp", "image/bmp", image("bmp"));

        Result<UploadVO> r = controller.upload(f);
        String url = r.getData().getUrl();

        // BMP 一律转 JPEG —— 证明压缩逻辑已接线到上传流程
        assertTrue(url.endsWith(".jpg"), "BMP 应转为 .jpg，实际 " + url);
        Path saved = dir.resolve(url.substring("/files/".length()));
        assertNotNull(ImageIO.read(saved.toFile()));
    }
}
