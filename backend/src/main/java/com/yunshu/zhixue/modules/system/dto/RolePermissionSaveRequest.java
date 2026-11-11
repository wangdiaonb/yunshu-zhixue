package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 保存角色权限（A-L05）：全量覆盖，允许传空数组清空权限。
 */
public record RolePermissionSaveRequest(

        @NotNull(message = "权限列表不能为空")
        @Size(max = 100, message = "权限数量不合法")
        List<Long> permissionIds
) {
}
