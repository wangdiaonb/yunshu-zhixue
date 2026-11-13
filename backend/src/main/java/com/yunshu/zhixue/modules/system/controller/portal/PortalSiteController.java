package com.yunshu.zhixue.modules.system.controller.portal;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingResponse;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端站点设置接口（P-H04）：返回管理端系统设置中的品牌信息，供门户全局渲染。
 */
@RestController
@RequestMapping(AppConstants.PORTAL_API_PREFIX + "/site")
@RequiredArgsConstructor
@Tag(name = "学员端-站点设置")
public class PortalSiteController {

    private final SystemSettingService systemSettingService;

    @GetMapping("/settings")
    @Operation(summary = "站点设置（公开）")
    public ApiResponse<SystemSettingResponse> getSettings() {
        return ApiResponse.ok(systemSettingService.getSettings());
    }
}
