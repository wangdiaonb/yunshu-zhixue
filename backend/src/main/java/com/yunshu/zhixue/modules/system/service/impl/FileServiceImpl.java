package com.yunshu.zhixue.modules.system.service.impl;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.config.storage.StorageService;
import com.yunshu.zhixue.modules.system.dto.FileUploadResponse;
import com.yunshu.zhixue.modules.system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 图片上传：校验 JPG/PNG、限制大小、等比缩放后写入对象存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final int MAX_IMAGE_SIDE = 512;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final StorageService storageService;

    @Override
    public FileUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.FILE_INVALID, "请选择图片文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.of(ErrorCode.FILE_INVALID, "图片大小不能超过 5MB");
        }
        String contentType = resolveContentType(file.getContentType());
        String format = "image/png".equals(contentType) ? "png" : "jpg";

        try {
            byte[] original = file.getBytes();
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
                throw BizException.of(ErrorCode.FILE_INVALID, "图片格式不正确");
            }

            double scale = Math.min(1.0, (double) MAX_IMAGE_SIDE / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int imageType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage target = new BufferedImage(width, height, imageType);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                if ("jpg".equals(format)) {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                }
                graphics.drawImage(source, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(target, format, output);
            byte[] bytes = output.toByteArray();

            String objectKey = "admin/images/" + LocalDate.now().format(MONTH_FORMATTER)
                    + "/" + UUID.randomUUID().toString().replace("-", "") + "." + format;
            String url = storageService.upload(objectKey, bytes, bytes.length, contentType);
            return new FileUploadResponse(url);
        } catch (BizException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("图片上传处理失败", ex);
            throw BizException.of(ErrorCode.FILE_INVALID, "图片处理失败，请稍后重试");
        }
    }

    private String resolveContentType(String contentType) {
        String value = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if ("image/png".equals(value)) {
            return "image/png";
        }
        if ("image/jpeg".equals(value) || "image/jpg".equals(value)) {
            return "image/jpeg";
        }
        throw BizException.of(ErrorCode.FILE_INVALID, "仅支持 JPG/PNG 图片");
    }
}
