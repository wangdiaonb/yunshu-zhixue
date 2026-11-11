package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.RoleCard;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RolePermissionSaveRequest;
import com.yunshu.zhixue.modules.system.service.RoleService;
import com.yunshu.zhixue.modules.system.service.SuperAdminGuard;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端角色管理接口（A-L01 ~ A-L03、A-L05），仅超级管理员可访问。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/roles")
@RequiredArgsConstructor
@Tag(name = "管理端-角色管理")
public class RoleAdminController {

    private final RoleService roleService;
    private final SuperAdminGuard superAdminGuard;

    @GetMapping
    @Operation(summary = "角色卡片列表")
    public ApiResponse<List<RoleCard>> list() {
        superAdminGuard.check();
        return ApiResponse.ok(roleService.listRoles());
    }

    @PostMapping
    @OperationLog(action = OperationAction.CREATE, targetType = "role", targetName = "#request.name")
    @Operation(summary = "新增角色")
    public ApiResponse<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        superAdminGuard.check();
        return ApiResponse.ok(roleService.createRole(request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(action = OperationAction.DELETE, targetType = "role", targetName = "#id")
    @Operation(summary = "删除角色")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        superAdminGuard.check();
        roleService.deleteRole(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/permissions")
    @OperationLog(action = OperationAction.UPDATE, targetType = "role_permission", targetName = "#id")
    @Operation(summary = "保存角色权限（全量覆盖）")
    public ApiResponse<Void> savePermissions(@PathVariable Long id,
                                             @Valid @RequestBody RolePermissionSaveRequest request) {
        superAdminGuard.check();
        roleService.savePermissions(id, request.permissionIds());
        return ApiResponse.ok();
    }
}
