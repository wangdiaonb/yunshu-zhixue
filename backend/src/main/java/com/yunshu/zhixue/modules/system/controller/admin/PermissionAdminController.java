package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionNode;
import com.yunshu.zhixue.modules.system.service.RoleService;
import com.yunshu.zhixue.modules.system.service.SuperAdminGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端权限树接口（A-L04），仅超级管理员可访问。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/permissions")
@RequiredArgsConstructor
@Tag(name = "管理端-角色管理")
public class PermissionAdminController {

    private final RoleService roleService;
    private final SuperAdminGuard superAdminGuard;

    @GetMapping("/tree")
    @Operation(summary = "权限树（含角色已勾选状态）")
    public ApiResponse<List<PermissionNode>> tree(
            @RequestParam(name = "roleId", required = false) Long roleId) {
        superAdminGuard.check();
        return ApiResponse.ok(roleService.permissionTree(roleId));
    }
}
