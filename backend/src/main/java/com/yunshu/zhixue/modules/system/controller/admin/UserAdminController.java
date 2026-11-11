package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetail;
import com.yunshu.zhixue.modules.system.dto.AdminUserItem;
import com.yunshu.zhixue.modules.system.dto.AdminUserQuery;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.system.dto.BatchUserStatusRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordResponse;
import com.yunshu.zhixue.modules.system.dto.RoleOption;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.modules.system.service.RoleService;
import com.yunshu.zhixue.security.RequirePermission;
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
 * 管理端用户管理接口（A-M01 ~ A-M08）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/users")
@RequiredArgsConstructor
@Tag(name = "管理端-用户管理")
public class UserAdminController {

    private final AdminUserService adminUserService;
    private final RoleService roleService;

    @GetMapping
    @RequirePermission("user:view")
    @Operation(summary = "用户分页列表")
    public ApiResponse<PageResult<AdminUserItem>> page(@Valid AdminUserQuery query) {
        return ApiResponse.ok(adminUserService.pageUsers(query));
    }

    /**
     * 用户管理的角色下拉选项（含超级管理员，前端按场景过滤）。
     */
    @GetMapping("/role-options")
    @RequirePermission("user:view")
    @Operation(summary = "角色下拉选项")
    public ApiResponse<List<RoleOption>> roleOptions() {
        return ApiResponse.ok(roleService.listOptions());
    }

    @GetMapping("/{id}/detail")
    @RequirePermission("user:view")
    @Operation(summary = "用户详情与最近操作")
    public ApiResponse<AdminUserDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.getUserDetail(id));
    }

    @PostMapping
    @RequirePermission("user:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "admin_user", targetName = "#request.username")
    @Operation(summary = "新增后台用户")
    public ApiResponse<Long> create(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.ok(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "admin_user", targetName = "#request.username")
    @Operation(summary = "编辑后台用户")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody AdminUserUpdateRequest request) {
        adminUserService.updateUser(id, request);
        return ApiResponse.ok();
    }

    @PutMapping("/batch-status")
    @RequirePermission("user:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "admin_user", targetName = "批量更新用户状态")
    @Operation(summary = "批量启用/禁用用户")
    public ApiResponse<Void> batchStatus(@Valid @RequestBody BatchUserStatusRequest request) {
        adminUserService.batchUpdateStatus(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("user:disable")
    @OperationLog(action = OperationAction.DELETE, targetType = "admin_user", targetName = "#id")
    @Operation(summary = "删除后台用户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/batch")
    @RequirePermission("user:disable")
    @OperationLog(action = OperationAction.DELETE, targetType = "admin_user", targetName = "批量删除用户")
    @Operation(summary = "批量删除后台用户")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody BatchIdsRequest request) {
        adminUserService.batchDelete(request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "admin_user", targetName = "#id")
    @Operation(summary = "重置为默认密码")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.ok(new ResetPasswordResponse(adminUserService.resetPassword(id)));
    }
}
