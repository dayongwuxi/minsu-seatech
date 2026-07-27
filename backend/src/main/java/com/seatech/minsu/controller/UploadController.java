package com.seatech.minsu.controller;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ImageCompressor;
import com.seatech.minsu.common.Result;
import com.seatech.minsu.common.ResultCode;
import com.seatech.minsu.dto.UploadVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/** 文件上传(用户端/管理端共用)，保存到 minsu.upload.dir，经 /files/** 静态映射访问 */
@RestController
public class UploadController {

    /** 允许的图片扩展名（小写，含点） */
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    @Value("${minsu.upload.dir}")
    private String uploadDir;

    @PostMapping({"/api/upload", "/api/admin/upload"})
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename();
        String ext = original != null && original.lastIndexOf('.') >= 0
                ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        String contentType = file.getContentType();
        boolean isImage = contentType != null && contentType.startsWith("image/");
        if (!isImage || !ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WEBP、BMP 格式的图片");
        }
        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件读取失败");
        }
        // 压缩/重编码，保证落库图片 ≤ 15MB（超限 GIF/WEBP 会在此抛业务异常）
        ImageCompressor.Result compressed = ImageCompressor.compress(rawBytes, ext);
        String filename = UUID.randomUUID().toString().replace("-", "") + compressed.ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), compressed.data);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败");
        }
        UploadVO vo = new UploadVO();
        vo.setUrl("/files/" + filename);
        return Result.ok(vo);
    }
}
