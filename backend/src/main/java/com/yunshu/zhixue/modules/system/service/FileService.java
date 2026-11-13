package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传服务（A-F01）。
 */
public interface FileService {

    /** 管理端图片（Logo / 默认头像）：等比缩放至最长边 512px（A-F01）。 */
    FileUploadResponse uploadImage(MultipartFile file);

    /** 学员头像：居中裁剪为 200×200（P-M05）。 */
    FileUploadResponse uploadAvatar(MultipartFile file);
}
