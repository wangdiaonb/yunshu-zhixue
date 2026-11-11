package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传服务（A-F01）。
 */
public interface FileService {

    FileUploadResponse uploadImage(MultipartFile file);
}
