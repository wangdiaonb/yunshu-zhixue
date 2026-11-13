package com.yunshu.zhixue.modules.system.controller.portal;

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
 * 学员端文件上传接口（P-M05）。
 */
@RestController
@RequestMapping(AppConstants.PORTAL_API_PREFIX + "/files")
@RequiredArgsConstructor
@Tag(name = "学员端-文件")
public class PortalFileController {

    private final FileService fileService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传学员头像（JPG/PNG，居中裁剪 200×200）")
    public ApiResponse<FileUploadResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(fileService.uploadAvatar(file));
    }
}
