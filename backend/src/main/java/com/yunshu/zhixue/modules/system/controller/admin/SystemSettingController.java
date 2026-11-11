package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端系统设置接口（A-T01 ~ A-T03）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/settings")
@RequiredArgsConstructor
@Tag(name = "管理端-系统设置")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @Operation(summary = "读取基本信息设置")
    public ApiResponse<SystemSettingResponse> get() {
        return ApiResponse.ok(systemSettingService.getSettings());
    }

    @PutMapping
    @RequirePermission("system:setting")
    @OperationLog(action = OperationAction.UPDATE, targetType = "system_setting", targetName = "基本信息设置")
    @Operation(summary = "保存基本信息设置")
    public ApiResponse<SystemSettingResponse> update(
            @Valid @RequestBody SystemSettingUpdateRequest request) {
        return ApiResponse.ok(systemSettingService.updateSettings(request));
    }

    @PostMapping("/reset")
    @RequirePermission("system:setting")
    @OperationLog(action = OperationAction.UPDATE, targetType = "system_setting", targetName = "恢复默认设置")
    @Operation(summary = "恢复默认设置")
    public ApiResponse<SystemSettingResponse> reset() {
        return ApiResponse.ok(systemSettingService.resetSettings());
    }
}
