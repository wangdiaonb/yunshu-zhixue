package com.yunshu.zhixue.config.storage;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * 基于 MinIO（S3 兼容）的对象存储实现。
 *
 * <p>Bucket 由 docker compose 的 minio-init 初始化；文件名一律由服务端生成。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public MinioStorageService(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String upload(String objectKey, byte[] data, long size, String contentType) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return url(objectKey);
        } catch (Exception ex) {
            log.error("对象上传失败：bucket={}, object={}", properties.getBucket(), objectKey, ex);
            throw BizException.of(ErrorCode.FILE_INVALID, "文件上传失败，请稍后重试");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("对象删除失败（忽略）：bucket={}, object={}", properties.getBucket(), objectKey, ex);
        }
    }

    @Override
    public String url(String objectKey) {
        String prefix = properties.getPublicEndpoint();
        if (prefix == null || prefix.isBlank()) {
            return objectKey;
        }
        return prefix.endsWith("/") ? prefix + objectKey : prefix + "/" + objectKey;
    }
}
