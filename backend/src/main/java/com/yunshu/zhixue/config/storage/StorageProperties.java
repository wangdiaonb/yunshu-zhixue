package com.yunshu.zhixue.config.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置（S3 兼容协议，本期自建 MinIO）。
 *
 * <p>密钥只允许来自环境变量，不落库、不进前端（架构 2.4）。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** 是否启用对象存储。 */
    private boolean enabled = true;

    /** 服务端访问端点，如 http://minio:9000。 */
    private String endpoint;

    /** 浏览器可访问的 URL 前缀（生产由 Nginx /files 直出）。 */
    private String publicEndpoint = "/files";

    private String accessKey;

    private String secretKey;

    /** 图片 Bucket（Logo、头像等公开资源）。 */
    private String bucket = "yszx";
}
