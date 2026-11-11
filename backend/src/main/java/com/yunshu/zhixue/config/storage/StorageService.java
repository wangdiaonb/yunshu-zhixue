package com.yunshu.zhixue.config.storage;

/**
 * 对象存储抽象：实现可切换（MinIO → 云 OSS/COS），业务只依赖本接口。
 */
public interface StorageService {

    /**
     * 上传对象。
     *
     * @param objectKey   服务端生成的对象键（UUID 文件名，不接受用户可控路径）
     * @param data        文件字节
     * @param size        字节数
     * @param contentType MIME 类型
     * @return 浏览器可访问的 URL
     */
    String upload(String objectKey, byte[] data, long size, String contentType);

    /** 删除对象；对象不存在时静默返回。 */
    void delete(String objectKey);

    /** 拼接对象访问 URL。 */
    String url(String objectKey);
}
