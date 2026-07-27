package com.seatech.minsu.service;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.dto.CaptchaVO;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** 图形验证码：内存存储(ConcurrentHashMap)，5 分钟过期，验证后即删除 */
@Service
public class CaptchaService {

    private static final long EXPIRE_MS = 5 * 60 * 1000L;
    private static final String CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    private record CodeEntry(String code, long expireAt) {
    }

    public CaptchaVO generate() {
        cleanExpired();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String code = sb.toString();
        String key = UUID.randomUUID().toString().replace("-", "");
        store.put(key, new CodeEntry(code, System.currentTimeMillis() + EXPIRE_MS));

        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            g.setFont(new Font("Arial", Font.BOLD, 26 + random.nextInt(4)));
            double theta = (random.nextInt(50) - 25) * Math.PI / 180;
            int x = 12 + i * 26;
            int y = 28 + random.nextInt(6);
            g.rotate(theta, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-theta, x, y);
        }
        g.dispose();

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            CaptchaVO vo = new CaptchaVO();
            vo.setKey(key);
            vo.setImageBase64("data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray()));
            return vo;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "验证码生成失败");
        }
    }

    public boolean verify(String key, String code) {
        if (key == null || code == null || code.isBlank()) {
            return false;
        }
        CodeEntry entry = store.remove(key);
        return entry != null && entry.expireAt() >= System.currentTimeMillis()
                && entry.code().equalsIgnoreCase(code.trim());
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt() < now);
    }
}
