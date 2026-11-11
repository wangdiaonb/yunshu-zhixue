package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.FileUploadResponse;
import com.yunshu.zhixue.modules.system.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端图片上传接口（A-F01）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/files")
@RequiredArgsConstructor
@Tag(name = "管理端-文件")
public class FileAdminController {

    private final FileService fileService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片（Logo/默认头像）")
    public ApiResponse<FileUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(fileService.uploadImage(file));
    }
}
